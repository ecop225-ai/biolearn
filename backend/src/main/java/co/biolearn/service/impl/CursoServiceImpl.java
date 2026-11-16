package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.CursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

/**
 * Gestión de cursos (Fase 1: Administrador).
 * Reglas principales:
 *  - Un curso nace en borrador; solo el Administrador lo publica (también los que en la Fase 2 cree un docente).
 *  - Para publicar necesita docente titular activo, descripción, y área y grado activos.
 *  - Transiciones: borrador→activo, activo→cerrado, cerrado→activo, (borrador|activo|cerrado)→archivado, archivado→borrador.
 *  - "Eliminar" es archivar: un curso archivado no se edita, no se ofrece en el catálogo ni admite contrataciones.
 */
@Service
@RequiredArgsConstructor
public class CursoServiceImpl implements CursoService {

    static final String BORRADOR = "borrador", ACTIVO = "activo", CERRADO = "cerrado", ARCHIVADO = "archivado";
    private static final Set<String> ESTADOS = Set.of(BORRADOR, ACTIVO, CERRADO, ARCHIVADO);
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            BORRADOR, Set.of(ACTIVO, ARCHIVADO),
            ACTIVO, Set.of(CERRADO, ARCHIVADO),
            CERRADO, Set.of(ACTIVO, ARCHIVADO),
            ARCHIVADO, Set.of(BORRADOR));
    private static final List<String> DIAS = List.of("lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo");
    private static final Set<String> ROLES_ADICIONALES = Set.of("colaborador", "invitado");

    private final CursoRepository cursoRepository;
    private final CursoDocenteRepository cursoDocenteRepository;
    private final HorarioCursoRepository horarioRepository;
    private final InstitucionCursoContratadoRepository contratacionRepository;
    private final GradoRepository gradoRepository;
    private final AreaAcademicaRepository areaRepository;
    private final PerfilDocenteRepository perfilDocenteRepository;
    private final PerfilAdminRepository perfilAdminRepository;
    private final InstitucionRepository institucionRepository;
    private final LeccionRepository leccionRepository;
    private final InscripcionRepository inscripcionRepository;

    // ── consulta ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CursoResponse> listar(String estado, Integer idArea, Integer idGrado, Integer idDocente, String busqueda) {
        List<Curso> cursos = (estado != null && !estado.isBlank())
                ? cursoRepository.findByEstadoOrderByNombreAsc(validarEstado(estado))
                : cursoRepository.findAllByOrderByNombreAsc();
        String q = busqueda != null ? normalizar(busqueda) : "";
        return cursos.stream()
                .filter(c -> idArea == null || c.getArea().getId().equals(idArea))
                .filter(c -> idGrado == null || c.getGrado().getId().equals(idGrado))
                .filter(c -> idDocente == null || (c.getDocenteTitular() != null && c.getDocenteTitular().getUsuario().getId().equals(idDocente)))
                .filter(c -> q.isEmpty() || normalizar(c.getNombre() + " " + Objects.toString(c.getDescripcion(), "") + " " + nombreDocente(c.getDocenteTitular())).contains(q))
                .map(this::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CursoResponse obtener(Integer id) {
        return aResponse(buscar(id));
    }

    // ── crear y editar ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public CursoResponse crear(CrearCursoRequest request, Integer idUsuarioAdmin) {
        String nombre = request.getNombre().trim();
        if (cursoRepository.existsByNombreIgnoreCaseAndEstadoNot(nombre, ARCHIVADO)) {
            throw new SolicitudInvalidaException("Ya existe un curso con ese nombre");
        }
        Curso curso = Curso.builder()
                .nombre(nombre)
                .area(buscarArea(request.getIdArea()))
                .grado(buscarGrado(request.getIdGrado()))
                .descripcion(texto(request.getDescripcion()))
                .objetivos(texto(request.getObjetivos()))
                .precio(request.getPrecio() != null ? request.getPrecio() : BigDecimal.ZERO)
                .soloInstitucional(Boolean.TRUE.equals(request.getSoloInstitucional()))
                .creditos(request.getCreditos() != null ? request.getCreditos() : 0)
                .horasEstimadas(request.getHorasEstimadas())
                .imagenPortada(limpiar(request.getImagenPortada()))
                .adminCreador(idUsuarioAdmin != null ? perfilAdminRepository.findByUsuario_Id(idUsuarioAdmin).orElse(null) : null)
                .estado(BORRADOR) // siempre nace en borrador: publicar es una decisión aparte
                .build();
        PerfilDocente titular = request.getIdDocenteTitular() != null ? buscarDocenteActivo(request.getIdDocenteTitular()) : null;
        curso.setDocenteTitular(titular);
        curso = cursoRepository.save(curso);
        if (titular != null) sincronizarTitular(curso, null, titular);
        return aResponse(curso);
    }

    @Override
    @Transactional
    public CursoResponse actualizar(Integer id, ActualizarCursoRequest request) {
        Curso curso = buscarEditable(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            String nombre = request.getNombre().trim();
            if (cursoRepository.existsByNombreIgnoreCaseAndEstadoNotAndIdNot(nombre, ARCHIVADO, id)) {
                throw new SolicitudInvalidaException("Ya existe otro curso con ese nombre");
            }
            curso.setNombre(nombre);
        }
        if (request.getIdArea() != null) curso.setArea(buscarArea(request.getIdArea()));
        if (request.getIdGrado() != null) curso.setGrado(buscarGrado(request.getIdGrado()));
        if (request.getDescripcion() != null) curso.setDescripcion(texto(request.getDescripcion()));
        if (request.getObjetivos() != null) curso.setObjetivos(texto(request.getObjetivos()));
        if (request.getPrecio() != null) curso.setPrecio(request.getPrecio());
        if (request.getSoloInstitucional() != null) curso.setSoloInstitucional(request.getSoloInstitucional());
        if (request.getCreditos() != null) curso.setCreditos(request.getCreditos());
        if (request.getHorasEstimadas() != null) curso.setHorasEstimadas(request.getHorasEstimadas());
        if (request.getImagenPortada() != null) curso.setImagenPortada(limpiar(request.getImagenPortada()));
        if (request.getIdDocenteTitular() != null) {
            PerfilDocente nuevo = buscarDocenteActivo(request.getIdDocenteTitular());
            PerfilDocente anterior = curso.getDocenteTitular();
            if (anterior == null || !anterior.getId().equals(nuevo.getId())) {
                curso.setDocenteTitular(nuevo);
                sincronizarTitular(curso, anterior, nuevo);
            }
        }
        return aResponse(cursoRepository.save(curso));
    }

    // ── estados ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CursoResponse cambiarEstado(Integer id, CambiarEstadoRequest request) {
        Curso curso = buscar(id);
        String nuevo = validarEstado(request.getEstado());
        String actual = curso.getEstado();
        if (nuevo.equals(actual)) {
            throw new SolicitudInvalidaException("El curso ya está en estado " + actual);
        }
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new SolicitudInvalidaException("No se puede pasar un curso de " + actual + " a " + nuevo
                    + ". Desde " + actual + " solo se permite: " + String.join(", ", new TreeSet<>(TRANSICIONES.get(actual))));
        }
        if (ACTIVO.equals(nuevo)) {
            List<String> pendientes = pendientesParaPublicar(curso);
            if (!pendientes.isEmpty()) {
                throw new SolicitudInvalidaException("El curso no se puede publicar todavía: " + String.join("; ", pendientes));
            }
        }
        curso.setEstado(nuevo);
        return aResponse(cursoRepository.save(curso));
    }

    // ── docentes ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CursoResponse agregarDocente(Integer id, AgregarDocenteCursoRequest request) {
        Curso curso = buscarEditable(id);
        String rol = request.getRol().trim().toLowerCase();
        if ("titular".equals(rol)) {
            throw new IllegalArgumentException("El docente titular se asigna editando el curso; aquí usa colaborador o invitado");
        }
        if (!ROLES_ADICIONALES.contains(rol)) {
            throw new IllegalArgumentException("Rol no válido. Usa: colaborador o invitado");
        }
        PerfilDocente docente = buscarDocenteActivo(request.getIdDocente());
        Optional<CursoDocente> existente = cursoDocenteRepository.findByCurso_IdAndDocente_Id(id, docente.getId());
        if (existente.isPresent() && "activo".equals(existente.get().getEstado())) {
            throw new SolicitudInvalidaException("Ese docente ya está asignado a este curso");
        }
        CursoDocente asignacion = existente.orElseGet(() -> CursoDocente.builder().curso(curso).docente(docente).build());
        asignacion.setRol(rol);
        asignacion.setEstado("activo"); // reactiva si había sido retirado antes
        cursoDocenteRepository.save(asignacion);
        return aResponse(curso);
    }

    @Override
    @Transactional
    public CursoResponse retirarDocente(Integer id, Integer idDocente) {
        Curso curso = buscarEditable(id);
        PerfilDocente docente = perfilDocenteRepository.findByUsuario_Id(idDocente)
                .orElseThrow(() -> new RecursoNoEncontradoException("El docente no está asignado a este curso"));
        CursoDocente asignacion = cursoDocenteRepository.findByCurso_IdAndDocente_Id(id, docente.getId())
                .filter(a -> "activo".equals(a.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("El docente no está asignado a este curso"));
        if ("titular".equals(asignacion.getRol())) {
            throw new SolicitudInvalidaException("No puedes retirar al docente titular; asigna otro titular editando el curso");
        }
        asignacion.setEstado("inactivo"); // se conserva el historial
        cursoDocenteRepository.save(asignacion);
        return aResponse(curso);
    }

    // ── horarios ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CursoResponse definirHorarios(Integer id, HorariosCursoRequest request) {
        Curso curso = buscarEditable(id);
        List<HorarioCurso> nuevos = new ArrayList<>();
        for (HorarioRequest h : request.getHorarios()) {
            String dia = normalizar(h.getDia());
            if (!DIAS.contains(dia)) {
                throw new IllegalArgumentException("Día no válido: " + h.getDia() + ". Usa: lunes, martes, miércoles, jueves, viernes, sábado o domingo");
            }
            if (!h.getHoraFin().isAfter(h.getHoraInicio())) {
                throw new IllegalArgumentException("En " + dia + " la hora de fin debe ser posterior a la de inicio");
            }
            for (HorarioCurso otro : nuevos) {
                boolean cruza = otro.getDiaSemana().equals(dia)
                        && h.getHoraInicio().isBefore(otro.getHoraFin()) && otro.getHoraInicio().isBefore(h.getHoraFin());
                if (cruza) throw new IllegalArgumentException("Los horarios del " + dia + " se cruzan");
            }
            nuevos.add(HorarioCurso.builder().curso(curso).diaSemana(dia).horaInicio(h.getHoraInicio()).horaFin(h.getHoraFin()).build());
        }
        horarioRepository.deleteByCurso_Id(id);
        horarioRepository.flush();
        horarioRepository.saveAll(nuevos);
        return aResponse(curso);
    }

    // ── instituciones ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public CursoResponse contratar(Integer id, ContratarCursoRequest request) {
        Curso curso = buscar(id);
        if (ARCHIVADO.equals(curso.getEstado())) {
            throw new SolicitudInvalidaException("Un curso archivado no se puede contratar");
        }
        Institucion institucion = institucionRepository.findById(request.getIdInstitucion())
                .orElseThrow(() -> new RecursoNoEncontradoException("La institución indicada no existe"));
        if (!"activa".equals(institucion.getEstado())) {
            throw new SolicitudInvalidaException("La institución no está activa; no puede contratar cursos");
        }
        if (contratacionRepository.existsByCurso_IdAndInstitucion_Id(id, institucion.getId())) {
            throw new SolicitudInvalidaException("La institución ya tiene contratado este curso");
        }
        contratacionRepository.save(InstitucionCursoContratado.builder().curso(curso).institucion(institucion)
                .codigoInscripcion(codigoInscripcionUnico()).build());
        return aResponse(curso);
    }

    @Override
    @Transactional
    public CursoResponse quitarContratacion(Integer id, Integer idInstitucion) {
        Curso curso = buscar(id);
        InstitucionCursoContratado contratacion = contratacionRepository.findByCurso_IdAndInstitucion_Id(id, idInstitucion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La institución no tiene contratado este curso"));
        if (inscripcionRepository.existsByContratacion_IdAndEstadoIn(contratacion.getId(), ESTADOS_INSCRITO)) {
            throw new SolicitudInvalidaException("No se puede quitar la contratación: hay estudiantes inscritos con el código de este curso");
        }
        contratacionRepository.delete(contratacion);
        return aResponse(curso);
    }

    /** Fase 3: el Administrador genera un código nuevo (por ejemplo, si el anterior se filtró). */
    @Override
    @Transactional
    public CursoResponse regenerarCodigo(Integer id, Integer idInstitucion) {
        Curso curso = buscar(id);
        InstitucionCursoContratado contratacion = contratacionRepository.findByCurso_IdAndInstitucion_Id(id, idInstitucion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La institución no tiene contratado este curso"));
        contratacion.setCodigoInscripcion(codigoInscripcionUnico());
        contratacionRepository.save(contratacion);
        return aResponse(curso);
    }

    static final java.util.Set<String> ESTADOS_INSCRITO = java.util.Set.of("pendiente_pago", "activa", "completada");

    private String codigoInscripcionUnico() {
        java.security.SecureRandom aleatorio = new java.security.SecureRandom();
        String alfabeto = co.biolearn.service.GeneradorCodigoAfiliacion.ALFABETO;
        for (int intento = 0; intento < 50; intento++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(alfabeto.charAt(aleatorio.nextInt(alfabeto.length())));
            if (!contratacionRepository.existsByCodigoInscripcion(sb.toString())) return sb.toString();
        }
        throw new IllegalStateException("No fue posible generar un código de inscripción único");
    }

    // ── catálogo público ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CursoCatalogoResponse> catalogo(Integer idArea, Integer idGrado, String busqueda) {
        String q = busqueda != null ? normalizar(busqueda) : "";
        return cursoRepository.findByEstadoOrderByNombreAsc(ACTIVO).stream()
                .filter(c -> !Boolean.TRUE.equals(c.getSoloInstitucional()))      // los cursos solo para colegios no se ofrecen al público
                .filter(c -> idArea == null || c.getArea().getId().equals(idArea))
                .filter(c -> idGrado == null || c.getGrado().getId().equals(idGrado))
                .filter(c -> q.isEmpty() || normalizar(c.getNombre() + " " + Objects.toString(c.getDescripcion(), "")).contains(q))
                .map(this::aCatalogo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CursoCatalogoResponse detalleCatalogo(Integer id) {
        Curso curso = cursoRepository.findById(id)
                .filter(c -> ACTIVO.equals(c.getEstado()) && !Boolean.TRUE.equals(c.getSoloInstitucional()))
                .orElseThrow(() -> new RecursoNoEncontradoException("El curso no existe o no está disponible"));
        return aCatalogo(curso);
    }

    // ── auxiliares ──────────────────────────────────────────────────────────

    private Curso buscar(Integer id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El curso indicado no existe"));
    }

    private Curso buscarEditable(Integer id) {
        Curso curso = buscar(id);
        if (ARCHIVADO.equals(curso.getEstado())) {
            throw new SolicitudInvalidaException("El curso está archivado; restáuralo a borrador para modificarlo");
        }
        return curso;
    }

    private AreaAcademica buscarArea(Integer id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El área académica indicada no existe"));
    }

    private Grado buscarGrado(Integer id) {
        return gradoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El grado indicado no existe"));
    }

    /** idUsuario = id de usuario del docente; debe tener perfil docente y cuenta y perfil activos. */
    private PerfilDocente buscarDocenteActivo(Integer idUsuario) {
        PerfilDocente docente = perfilDocenteRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("El docente indicado no existe"));
        if (docente.getUsuario().getEstado() != EstadoUsuario.activo || docente.getEstado() != EstadoPerfilAmplio.activo) {
            throw new SolicitudInvalidaException("El docente no está activo; solo se pueden asignar docentes con la cuenta activa");
        }
        return docente;
    }

    /** Mantiene curso_docente alineado con curso.id_docente_titular. */
    private void sincronizarTitular(Curso curso, PerfilDocente anterior, PerfilDocente nuevo) {
        if (anterior != null) {
            cursoDocenteRepository.findByCurso_IdAndDocente_Id(curso.getId(), anterior.getId()).ifPresent(a -> {
                a.setEstado("inactivo");
                cursoDocenteRepository.save(a);
            });
        }
        CursoDocente asignacion = cursoDocenteRepository.findByCurso_IdAndDocente_Id(curso.getId(), nuevo.getId())
                .orElseGet(() -> CursoDocente.builder().curso(curso).docente(nuevo).build());
        asignacion.setRol("titular");
        asignacion.setEstado("activo");
        cursoDocenteRepository.save(asignacion);
    }

    /** Público para que el servicio de contenido (Fase 2) reutilice la misma regla. */
    public List<String> pendientesParaPublicar(Curso curso) {
        List<String> faltan = new ArrayList<>();
        PerfilDocente t = curso.getDocenteTitular();
        if (t == null) faltan.add("asigna un docente titular");
        else if (t.getUsuario().getEstado() != EstadoUsuario.activo || t.getEstado() != EstadoPerfilAmplio.activo)
            faltan.add("el docente titular no está activo");
        if (curso.getDescripcion() == null || curso.getDescripcion().isBlank()) faltan.add("agrega una descripción");
        if (!"activo".equals(curso.getArea().getEstado())) faltan.add("el área académica está inactiva");
        if (!"activo".equals(curso.getGrado().getEstado())) faltan.add("el grado está inactivo");
        if (curso.getId() != null && leccionRepository.countByCurso_IdAndEstado(curso.getId(), "activa") == 0)
            faltan.add("agrega al menos una lección activa"); // Fase 2: el contenido lo construyen los docentes
        return faltan;
    }

    private String validarEstado(String estado) {
        String e = estado.trim().toLowerCase();
        if (!ESTADOS.contains(e)) {
            throw new IllegalArgumentException("Estado de curso no válido. Usa: borrador, activo, cerrado o archivado");
        }
        return e;
    }

    private static String nombreDocente(PerfilDocente d) {
        return d == null ? "" : d.getUsuario().getNombre() + " " + d.getUsuario().getApellido();
    }

    /** descripcion y objetivos son TEXT NOT NULL en la base: un campo vacío se guarda como "" y no como NULL. */
    private static String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /** Muestra "" (o 0 en créditos) como "sin dato" en las respuestas. */
    private static String sinVacio(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static Integer sinCero(Integer valor) {
        return valor == null || valor == 0 ? null : valor;
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private List<CursoResponse.Horario> horarios(Integer idCurso) {
        return horarioRepository.findByCurso_Id(idCurso).stream()
                .sorted(Comparator.comparingInt((HorarioCurso h) -> DIAS.indexOf(h.getDiaSemana())).thenComparing(HorarioCurso::getHoraInicio))
                .map(h -> CursoResponse.Horario.builder().dia(h.getDiaSemana()).horaInicio(h.getHoraInicio()).horaFin(h.getHoraFin()).build())
                .toList();
    }

    CursoResponse aResponse(Curso c) {
        PerfilDocente t = c.getDocenteTitular();
        List<CursoResponse.DocenteCurso> docentes = cursoDocenteRepository.findByCurso_IdOrderByAsignadoEnAsc(c.getId()).stream()
                .filter(a -> "activo".equals(a.getEstado()))
                .map(a -> CursoResponse.DocenteCurso.builder()
                        .idDocente(a.getDocente().getUsuario().getId())
                        .nombre(nombreDocente(a.getDocente()))
                        .correo(a.getDocente().getUsuario().getCorreo())
                        .rol(a.getRol()).estado(a.getEstado()).build())
                .toList();
        List<CursoResponse.InstitucionContratada> instituciones = contratacionRepository.findByCurso_IdOrderByContratadoEnAsc(c.getId()).stream()
                .map(x -> CursoResponse.InstitucionContratada.builder()
                        .idInstitucion(x.getInstitucion().getId()).nombre(x.getInstitucion().getNombre())
                        .estado(x.getInstitucion().getEstado())
                        .codigoInscripcion(x.getCodigoInscripcion())
                        .inscritos(inscripcionRepository.countByContratacion_IdAndEstadoIn(x.getId(), ESTADOS_INSCRITO))
                        .contratadoEn(x.getContratadoEn() != null ? x.getContratadoEn().toLocalDate() : null).build())
                .toList();
        return CursoResponse.builder()
                .id(c.getId()).nombre(c.getNombre()).descripcion(sinVacio(c.getDescripcion())).objetivos(sinVacio(c.getObjetivos()))
                .imagenPortada(c.getImagenPortada()).precio(c.getPrecio()).creditos(sinCero(c.getCreditos()))
                .soloInstitucional(Boolean.TRUE.equals(c.getSoloInstitucional()))
                .codigo(c.getCodigo()).color(c.getColor()).infoEstudiante(c.getInfoEstudiante())
                .horasEstimadas(c.getHorasEstimadas()).estado(c.getEstado())
                .idArea(c.getArea().getId()).area(c.getArea().getNombre())
                .idGrado(c.getGrado().getId()).grado(c.getGrado().getNombre())
                .idDocenteTitular(t != null ? t.getUsuario().getId() : null)
                .docenteTitular(t != null ? nombreDocente(t) : null)
                .creadoPor(c.getAdminCreador() != null || t == null ? "Administrador" : "Docente")
                .fechaCreacion(c.getCreadoEn() != null ? c.getCreadoEn().toLocalDate() : null)
                .docentes(docentes).horarios(horarios(c.getId())).instituciones(instituciones)
                .pendientesParaPublicar(BORRADOR.equals(c.getEstado()) ? pendientesParaPublicar(c) : List.of())
                .totalLecciones(leccionRepository.countByCurso_Id(c.getId()))
                .leccionesActivas(leccionRepository.countByCurso_IdAndEstado(c.getId(), "activa"))
                .build();
    }

    private CursoCatalogoResponse aCatalogo(Curso c) {
        return CursoCatalogoResponse.builder()
                .id(c.getId()).nombre(c.getNombre()).descripcion(sinVacio(c.getDescripcion())).objetivos(sinVacio(c.getObjetivos()))
                .imagenPortada(c.getImagenPortada()).precio(c.getPrecio()).creditos(sinCero(c.getCreditos()))
                .codigo(c.getCodigo()).color(c.getColor()).infoEstudiante(c.getInfoEstudiante())
                .horasEstimadas(c.getHorasEstimadas()).area(c.getArea().getNombre()).grado(c.getGrado().getNombre())
                .docenteTitular(nombreDocente(c.getDocenteTitular()).isBlank() ? null : nombreDocente(c.getDocenteTitular()))
                .horarios(horarios(c.getId()))
                .build();
    }
}
