package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.ContenidoCursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contenido de los cursos (Fase 2).
 * Permisos: el titular y los colaboradores editan; los invitados y el Administrador solo consultan;
 * los demás docentes reciben 403. Un curso archivado no se modifica.
 * Nada se borra: lecciones, materiales y recursos se inactivan o archivan.
 */
@Service
@RequiredArgsConstructor
public class ContenidoCursoServiceImpl implements ContenidoCursoService {

    static final Map<String, Set<String>> TRANSICIONES_LECCION = Map.of(
            "borrador", Set.of("activa", "archivada"),
            "activa", Set.of("inactiva", "archivada"),
            "inactiva", Set.of("activa", "archivada"),
            "archivada", Set.of("borrador"));
    private static final Set<String> TIPOS_LECCION = Set.of("teoria", "practica", "evaluacion", "mixta");
    private static final Set<String> TIPOS_MATERIAL = Set.of("lectura", "video", "laboratorio", "juego", "documento", "articulo");
    private static final Set<String> ESTADOS_MATERIAL = Set.of("activo", "inactivo", "archivado");
    private static final Set<String> TIPOS_RECURSO = Set.of("video", "documento", "imagen", "audio", "enlace", "interactivo");
    private static final Set<String> NIVELES = Set.of("basico", "intermedio", "avanzado");
    private static final Pattern YOUTUBE = Pattern.compile(
            "^https?://(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})(?:[?&#].*)?$");

    private final CursoRepository cursoRepository;
    private final CursoDocenteRepository cursoDocenteRepository;
    private final LeccionRepository leccionRepository;
    private final MaterialContenidoRepository materialRepository;
    private final RecursoRepository recursoRepository;
    private final PerfilDocenteRepository perfilDocenteRepository;
    private final AreaAcademicaRepository areaRepository;
    private final GradoRepository gradoRepository;
    private final ArticuloCientificoRepository articuloRepository;
    private final RecursoApoyoRepository apoyoRepository;
    private final co.biolearn.service.ArchivoFirma archivoFirma;
    private final CursoServiceImpl cursoService; // reutiliza las reglas de publicación (pendientes)

    // ── cursos del docente ──────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CursoDocenteResponse> misCursos(Usuario usuario) {
        PerfilDocente docente = perfil(usuario);
        return cursoDocenteRepository.findByDocente_IdAndEstado(docente.getId(), "activo").stream()
                .sorted(Comparator.comparing(a -> a.getCurso().getNombre()))
                .map(a -> aCursoDocente(a.getCurso(), a.getRol()))
                .toList();
    }

    @Override
    @Transactional
    public CursoDocenteResponse proponerCurso(CursoDocenteRequest r, Usuario usuario) {
        PerfilDocente docente = perfil(usuario);
        if (vacio(r.getNombre()) || r.getIdArea() == null || r.getIdGrado() == null) {
            throw new IllegalArgumentException("Nombre, área y grado son obligatorios para proponer un curso");
        }
        String nombre = r.getNombre().trim();
        if (cursoRepository.existsByNombreIgnoreCaseAndEstadoNot(nombre, "archivado")) {
            throw new SolicitudInvalidaException("Ya existe un curso con ese nombre");
        }
        String codigo = codigoValido(r.getCodigo(), null);
        Curso curso = Curso.builder()
                .nombre(nombre).codigo(codigo)
                .color(vacio(r.getColor()) ? null : r.getColor().trim())
                .infoEstudiante(vacio(r.getInfoEstudiante()) ? null : co.biolearn.service.ContenidoHtmlSanitizer.limpiar(r.getInfoEstudiante()))
                .area(areaRepository.findById(r.getIdArea()).orElseThrow(() -> new RecursoNoEncontradoException("El área académica indicada no existe")))
                .grado(gradoRepository.findById(r.getIdGrado()).orElseThrow(() -> new RecursoNoEncontradoException("El grado indicado no existe")))
                .descripcion(texto(r.getDescripcion())).objetivos(texto(r.getObjetivos()))
                .imagenPortada(vacio(r.getImagenPortada()) ? null : r.getImagenPortada().trim())
                .creditos(r.getCreditos() != null ? r.getCreditos() : 0)
                .horasEstimadas(r.getHorasEstimadas())
                .precio(BigDecimal.ZERO)          // el precio lo define el Administrador
                .docenteTitular(docente)          // quien propone queda como titular
                .estado("borrador")               // solo el Administrador publica (RF-035)
                .build();
        curso = cursoRepository.save(curso);
        cursoDocenteRepository.save(CursoDocente.builder().curso(curso).docente(docente).rol("titular").estado("activo").build());
        return aCursoDocente(curso, "titular");
    }

    @Override
    @Transactional
    public CursoDocenteResponse actualizarCursoDocente(Integer idCurso, CursoDocenteRequest r, Usuario usuario) {
        Curso curso = buscarCurso(idCurso);
        String rol = rolEnCurso(curso, usuario);
        if (!"titular".equals(rol)) {
            throw new SinPermisoException("Solo el docente titular puede editar los datos del curso");
        }
        noArchivado(curso);
        if (!vacio(r.getNombre())) {
            String nombre = r.getNombre().trim();
            if (cursoRepository.existsByNombreIgnoreCaseAndEstadoNotAndIdNot(nombre, "archivado", idCurso)) {
                throw new SolicitudInvalidaException("Ya existe otro curso con ese nombre");
            }
            curso.setNombre(nombre);
        }
        if (r.getIdArea() != null) curso.setArea(areaRepository.findById(r.getIdArea()).orElseThrow(() -> new RecursoNoEncontradoException("El área académica indicada no existe")));
        if (r.getIdGrado() != null) curso.setGrado(gradoRepository.findById(r.getIdGrado()).orElseThrow(() -> new RecursoNoEncontradoException("El grado indicado no existe")));
        if (r.getDescripcion() != null) curso.setDescripcion(texto(r.getDescripcion()));
        if (r.getObjetivos() != null) curso.setObjetivos(texto(r.getObjetivos()));
        if (r.getImagenPortada() != null) curso.setImagenPortada(vacio(r.getImagenPortada()) ? null : r.getImagenPortada().trim());
        if (r.getCreditos() != null) curso.setCreditos(r.getCreditos());
        if (r.getHorasEstimadas() != null) curso.setHorasEstimadas(r.getHorasEstimadas());
        if (r.getCodigo() != null) curso.setCodigo(codigoValido(r.getCodigo(), idCurso));
        if (r.getColor() != null) curso.setColor(vacio(r.getColor()) ? null : r.getColor().trim());
        if (r.getInfoEstudiante() != null) curso.setInfoEstudiante(vacio(r.getInfoEstudiante()) ? null : co.biolearn.service.ContenidoHtmlSanitizer.limpiar(r.getInfoEstudiante()));
        return aCursoDocente(cursoRepository.save(curso), rol);
    }

    // ── lecciones ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LeccionResponse> listarLecciones(Integer idCurso, Usuario usuario) {
        Curso curso = buscarCurso(idCurso);
        exigirLectura(curso, usuario);
        return leccionRepository.findByCurso_IdOrderByOrdenAsc(idCurso).stream().map(l -> aLeccion(l, false)).toList();
    }

    @Override
    @Transactional
    public LeccionResponse crearLeccion(Integer idCurso, LeccionRequest r, Usuario usuario) {
        Curso curso = buscarCurso(idCurso);
        exigirEdicion(curso, usuario);
        if (vacio(r.getNombre())) throw new IllegalArgumentException("El nombre de la lección es obligatorio");
        Leccion leccion = Leccion.builder()
                .curso(curso).nombre(r.getNombre().trim())
                .descripcion(vacio(r.getDescripcion()) ? null : r.getDescripcion().trim())
                .imagenPortada(vacio(r.getImagenPortada()) ? null : r.getImagenPortada().trim())
                .tipo(r.getTipo() != null ? validar(r.getTipo(), TIPOS_LECCION, "Tipo de lección no válido. Usa: teoria, practica, evaluacion o mixta") : "teoria")
                .orden(leccionRepository.maxOrden(idCurso) + 1) // queda al final
                .estado("borrador")                              // también en cursos publicados
                .build();
        if (r.getIdPrerequisito() != null && r.getIdPrerequisito() != 0) {
            leccion.setPrerequisito(prerequisitoValido(curso, r.getIdPrerequisito(), null));
        }
        return aLeccion(leccionRepository.save(leccion), true);
    }

    @Override
    @Transactional(readOnly = true)
    public LeccionResponse detalleLeccion(Integer idLeccion, Usuario usuario) {
        Leccion leccion = buscarLeccion(idLeccion);
        exigirLectura(leccion.getCurso(), usuario);
        return aLeccion(leccion, true);
    }

    @Override
    @Transactional
    public LeccionResponse actualizarLeccion(Integer idLeccion, LeccionRequest r, Usuario usuario) {
        Leccion leccion = buscarLeccion(idLeccion);
        exigirEdicion(leccion.getCurso(), usuario);
        if (!vacio(r.getNombre())) leccion.setNombre(r.getNombre().trim());
        if (r.getDescripcion() != null) leccion.setDescripcion(vacio(r.getDescripcion()) ? null : r.getDescripcion().trim());
        if (r.getImagenPortada() != null) leccion.setImagenPortada(vacio(r.getImagenPortada()) ? null : r.getImagenPortada().trim());
        if (r.getTipo() != null) leccion.setTipo(validar(r.getTipo(), TIPOS_LECCION, "Tipo de lección no válido. Usa: teoria, practica, evaluacion o mixta"));
        if (r.getIdPrerequisito() != null) {
            leccion.setPrerequisito(r.getIdPrerequisito() == 0 ? null : prerequisitoValido(leccion.getCurso(), r.getIdPrerequisito(), leccion));
        }
        return aLeccion(leccionRepository.save(leccion), true);
    }

    @Override
    @Transactional
    public LeccionResponse cambiarEstadoLeccion(Integer idLeccion, CambiarEstadoRequest r, Usuario usuario) {
        Leccion leccion = buscarLeccion(idLeccion);
        exigirEdicion(leccion.getCurso(), usuario);
        String nuevo = validar(r.getEstado(), TRANSICIONES_LECCION.keySet(), "Estado de lección no válido. Usa: borrador, activa, inactiva o archivada");
        String actual = leccion.getEstado();
        if (nuevo.equals(actual)) throw new SolicitudInvalidaException("La lección ya está en estado " + actual);
        if (!TRANSICIONES_LECCION.get(actual).contains(nuevo)) {
            throw new SolicitudInvalidaException("No se puede pasar una lección de " + actual + " a " + nuevo
                    + ". Desde " + actual + " solo se permite: " + String.join(", ", new TreeSet<>(TRANSICIONES_LECCION.get(actual))));
        }
        if ("activa".equals(nuevo) && materialRepository.countByLeccion_IdAndEstado(idLeccion, "activo") == 0) {
            throw new SolicitudInvalidaException("La lección no se puede activar sin al menos un material activo");
        }
        if ("activa".equals(actual) && "activo".equals(leccion.getCurso().getEstado())
                && leccionRepository.countByCurso_IdAndEstado(leccion.getCurso().getId(), "activa") <= 1) {
            // un curso publicado nunca se queda sin lecciones: los estudiantes podrían inscribirse a un curso vacío
            throw new SolicitudInvalidaException("Es la única lección activa de un curso publicado; activa otra lección antes o pide al Administrador cerrar el curso");
        }
        if ("archivada".equals(nuevo) && leccionRepository.existsByPrerequisito_IdAndEstado(idLeccion, "activa")) {
            throw new SolicitudInvalidaException("No se puede archivar: es prerrequisito de una lección activa");
        }
        leccion.setEstado(nuevo);
        return aLeccion(leccionRepository.save(leccion), true);
    }

    @Override
    @Transactional
    public List<LeccionResponse> reordenarLecciones(Integer idCurso, OrdenLeccionesRequest r, Usuario usuario) {
        Curso curso = buscarCurso(idCurso);
        exigirEdicion(curso, usuario);
        List<Leccion> todas = leccionRepository.findByCurso_IdOrderByOrdenAsc(idCurso);
        List<Leccion> vigentes = todas.stream().filter(l -> !"archivada".equals(l.getEstado())).toList();
        Set<Integer> esperadas = new HashSet<>(vigentes.stream().map(Leccion::getId).toList());
        if (r.getLecciones().size() != esperadas.size() || !esperadas.equals(new HashSet<>(r.getLecciones()))) {
            throw new IllegalArgumentException("Envía exactamente las " + esperadas.size() + " lecciones no archivadas del curso, sin repetir");
        }
        Map<Integer, Integer> posicion = new HashMap<>();
        for (int i = 0; i < r.getLecciones().size(); i++) posicion.put(r.getLecciones().get(i), i);
        for (Leccion l : vigentes) {
            Leccion pre = l.getPrerequisito();
            if (pre != null && posicion.containsKey(pre.getId()) && posicion.get(pre.getId()) > posicion.get(l.getId())) {
                throw new SolicitudInvalidaException("El nuevo orden deja «" + l.getNombre() + "» antes de su prerrequisito «" + pre.getNombre() + "»");
            }
        }
        // Dos pasos para respetar el orden único por curso (uq_leccion_orden)
        for (Leccion l : todas) l.setOrden(l.getOrden() + 10000);
        leccionRepository.saveAllAndFlush(todas);
        Map<Integer, Leccion> porId = new HashMap<>();
        todas.forEach(l -> porId.put(l.getId(), l));
        int orden = 1;
        for (Integer id : r.getLecciones()) porId.get(id).setOrden(orden++);
        for (Leccion l : todas) if ("archivada".equals(l.getEstado())) l.setOrden(orden++);
        leccionRepository.saveAllAndFlush(todas);
        return leccionRepository.findByCurso_IdOrderByOrdenAsc(idCurso).stream().map(l -> aLeccion(l, false)).toList();
    }

    // ── materiales ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LeccionResponse crearMaterial(Integer idLeccion, MaterialRequest r, Usuario usuario) {
        Leccion leccion = buscarLeccion(idLeccion);
        exigirEdicion(leccion.getCurso(), usuario);
        if ("archivada".equals(leccion.getEstado())) throw new SolicitudInvalidaException("La lección está archivada; restáurala para agregar material");
        if (vacio(r.getNombre()) || vacio(r.getTipo())) throw new IllegalArgumentException("Nombre y tipo del material son obligatorios");
        String tipo = validar(r.getTipo(), TIPOS_MATERIAL, "Tipo de material no válido. Usa: lectura, video, laboratorio, juego, documento o articulo");
        if ("articulo".equals(tipo) && vacio(r.getAutores())) throw new IllegalArgumentException("Los autores del artículo son obligatorios");
        PerfilDocente docente = perfil(usuario);
        MaterialContenido m = MaterialContenido.builder()
                .leccion(leccion).docente(docente).nombre(r.getNombre().trim()).tipo(tipo)
                .descripcion(vacio(r.getDescripcion()) ? null : r.getDescripcion().trim())
                .orden(materialRepository.maxOrden(idLeccion) + 1).estado("activo").build();
        aplicarCamposDelMaterial(m, r);
        materialRepository.save(m); // un solo guardado; m recibe su id
        guardarRecursoYArticulo(m, r, docente);
        return aLeccion(leccion, true);
    }

    @Override
    @Transactional
    public LeccionResponse actualizarMaterial(Integer idMaterial, MaterialRequest r, Usuario usuario) {
        MaterialContenido m = buscarMaterial(idMaterial);
        exigirEdicion(m.getLeccion().getCurso(), usuario);
        if (!vacio(r.getNombre())) m.setNombre(r.getNombre().trim());
        if (r.getTipo() != null) m.setTipo(validar(r.getTipo(), TIPOS_MATERIAL, "Tipo de material no válido. Usa: lectura, video, laboratorio, juego, documento o articulo"));
        if (r.getDescripcion() != null) m.setDescripcion(vacio(r.getDescripcion()) ? null : r.getDescripcion().trim());
        aplicarCamposDelMaterial(m, r);
        materialRepository.save(m);
        guardarRecursoYArticulo(m, r, perfil(usuario));
        return aLeccion(m.getLeccion(), true);
    }

    @Override
    @Transactional
    public LeccionResponse cambiarEstadoMaterial(Integer idMaterial, CambiarEstadoRequest r, Usuario usuario) {
        MaterialContenido m = buscarMaterial(idMaterial);
        Leccion leccion = m.getLeccion();
        exigirEdicion(leccion.getCurso(), usuario);
        String nuevo = validar(r.getEstado(), ESTADOS_MATERIAL, "Estado de material no válido. Usa: activo, inactivo o archivado");
        if (nuevo.equals(m.getEstado())) throw new SolicitudInvalidaException("El material ya está en estado " + nuevo);
        boolean ultimoActivo = "activo".equals(m.getEstado()) && materialRepository.countByLeccion_IdAndEstado(leccion.getId(), "activo") <= 1;
        if (!"activo".equals(nuevo) && ultimoActivo && "activa".equals(leccion.getEstado())) {
            throw new SolicitudInvalidaException("Es el único material activo de una lección activa; inactiva primero la lección o agrega otro material");
        }
        m.setEstado(nuevo);
        materialRepository.save(m);
        return aLeccion(leccion, true);
    }

    // ── recursos ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LeccionResponse crearRecurso(Integer idMaterial, RecursoRequest r, Usuario usuario) {
        MaterialContenido m = buscarMaterial(idMaterial);
        exigirEdicion(m.getLeccion().getCurso(), usuario);
        if ("archivado".equals(m.getEstado())) throw new SolicitudInvalidaException("El material está archivado; actívalo para agregar recursos");
        if (vacio(r.getNombre()) || vacio(r.getUrl()) || vacio(r.getTipoRecurso())) {
            throw new IllegalArgumentException("Nombre, URL y tipo del recurso son obligatorios");
        }
        String tipo = validar(r.getTipoRecurso(), TIPOS_RECURSO, "Tipo de recurso no válido. Usa: video, documento, imagen, audio, enlace o interactivo");
        Recurso nuevo = Recurso.builder()
                .material(m).docente(perfil(usuario)).nombre(r.getNombre().trim())
                .descripcion(vacio(r.getDescripcion()) ? null : r.getDescripcion().trim())
                .tipoRecurso(tipo).url(normalizarUrl(r.getUrl(), tipo))
                .duracionMin(r.getDuracionMin())
                .nivelAcademico(vacio(r.getNivelAcademico()) ? null : validar(r.getNivelAcademico(), NIVELES, "Nivel no válido. Usa: basico, intermedio o avanzado"))
                .orden(recursoRepository.maxOrden(idMaterial) + 1).estado("activo").build();
        recursoRepository.save(nuevo);
        sincronizarApoyo(nuevo, "complementario", usuario);   // recurso de apoyo del material
        return aLeccion(m.getLeccion(), true);
    }

    @Override
    @Transactional
    public LeccionResponse actualizarRecurso(Integer idRecurso, RecursoRequest r, Usuario usuario) {
        Recurso rec = buscarRecurso(idRecurso);
        exigirEdicion(rec.getMaterial().getLeccion().getCurso(), usuario);
        if (!vacio(r.getNombre())) rec.setNombre(r.getNombre().trim());
        if (r.getDescripcion() != null) rec.setDescripcion(vacio(r.getDescripcion()) ? null : r.getDescripcion().trim());
        if (r.getTipoRecurso() != null) rec.setTipoRecurso(validar(r.getTipoRecurso(), TIPOS_RECURSO, "Tipo de recurso no válido. Usa: video, documento, imagen, audio, enlace o interactivo"));
        if (r.getUrl() != null || r.getTipoRecurso() != null) rec.setUrl(normalizarUrl(r.getUrl() != null ? r.getUrl() : rec.getUrl(), rec.getTipoRecurso()));
        if (r.getDuracionMin() != null) rec.setDuracionMin(r.getDuracionMin());
        if (r.getNivelAcademico() != null) rec.setNivelAcademico(vacio(r.getNivelAcademico()) ? null : validar(r.getNivelAcademico(), NIVELES, "Nivel no válido. Usa: basico, intermedio o avanzado"));
        recursoRepository.save(rec);
        sincronizarApoyo(rec, null, usuario);
        return aLeccion(rec.getMaterial().getLeccion(), true);
    }

    @Override
    @Transactional
    public LeccionResponse cambiarEstadoRecurso(Integer idRecurso, CambiarEstadoRequest r, Usuario usuario) {
        Recurso rec = buscarRecurso(idRecurso);
        exigirEdicion(rec.getMaterial().getLeccion().getCurso(), usuario);
        String nuevo = validar(r.getEstado(), Set.of("activo", "inactivo"), "Estado de recurso no válido. Usa: activo o inactivo");
        if (nuevo.equals(rec.getEstado())) throw new SolicitudInvalidaException("El recurso ya está en estado " + nuevo);
        rec.setEstado(nuevo);
        recursoRepository.save(rec);
        sincronizarApoyo(rec, null, usuario);
        return aLeccion(rec.getMaterial().getLeccion(), true);
    }

    // ── datos propios de cada tipo de material (diseño original del prototipo) ──

    /**
     * Campos del propio material según su tipo (sin guardar): lectura → contenido y color de fondo;
     * video, laboratorio y juego → guía de observación. Solo se aplican los campos enviados (null = sin cambios).
     */
    void aplicarCamposDelMaterial(MaterialContenido m, MaterialRequest r) {
        String tipo = m.getTipo();
        if ("lectura".equals(tipo)) {
            if (r.getContenido() != null) m.setContenido(vacio(r.getContenido()) ? null : co.biolearn.service.ContenidoHtmlSanitizer.limpiar(r.getContenido()));
            if (r.getColorFondo() != null) m.setColorFondo(vacio(r.getColorFondo()) ? null : r.getColorFondo().trim());
        }
        if (Set.of("video", "laboratorio", "juego").contains(tipo) && r.getGuia() != null) {
            m.setGuia(vacio(r.getGuia()) ? null : co.biolearn.service.ContenidoHtmlSanitizer.limpiar(r.getGuia()));
        }
    }

    /**
     * Datos que viven en otras tablas (el material ya debe estar guardado): video, laboratorio, juego y documento →
     * enlace, proveedor y duración en el recurso principal; artículo → articulo_cientifico.
     */
    void guardarRecursoYArticulo(MaterialContenido m, MaterialRequest r, PerfilDocente docente) {
        String tipo = m.getTipo();
        if (Set.of("video", "laboratorio", "juego", "documento").contains(tipo)
                && (r.getUrl() != null || r.getProveedor() != null || r.getDuracionMin() != null)) {
            String tipoRecurso = "video".equals(tipo) ? "video" : ("documento".equals(tipo) ? "documento" : "interactivo");
            Recurso p = recursoPrincipal(m);
            boolean sinUrl = vacio(r.getUrl());
            if (p == null && sinUrl) return; // un recurso exige URL: no hay nada que guardar todavía
            if (p == null) {
                p = Recurso.builder().material(m).docente(docente).orden(recursoRepository.maxOrden(m.getId()) + 1).build();
            }
            p.setNombre(m.getNombre());
            p.setTipoRecurso(tipoRecurso);
            if (r.getUrl() != null) {
                if (sinUrl) p.setEstado("inactivo");        // se quitó el enlace: el recurso queda inactivo, no se borra
                else { p.setUrl(normalizarUrl(r.getUrl(), tipoRecurso)); p.setEstado("activo"); }
            }
            if (r.getProveedor() != null) p.setDescripcion(vacio(r.getProveedor()) ? null : r.getProveedor().trim());
            if (r.getDuracionMin() != null) p.setDuracionMin(r.getDuracionMin());
            recursoRepository.save(p);
            sincronizarApoyo(p, "primario", docente.getUsuario());
        }
        if ("articulo".equals(tipo)) {
            ArticuloCientifico a = articuloRepository.findFirstByMaterial_Id(m.getId())
                    .orElseGet(() -> ArticuloCientifico.builder().material(m).docente(docente).build());
            a.setTitulo(m.getNombre());
            a.setResumen(m.getDescripcion());
            if (r.getAutores() != null) a.setAutores(r.getAutores().trim());
            if (vacio(a.getAutores())) throw new IllegalArgumentException("Los autores del artículo son obligatorios");
            if (r.getRevista() != null) a.setRevista(vacio(r.getRevista()) ? null : r.getRevista().trim());
            if (r.getDoi() != null) a.setDoi(vacio(r.getDoi()) ? null : r.getDoi().trim());
            if (r.getUrl() != null) a.setUrl(vacio(r.getUrl()) ? null : normalizarUrl(r.getUrl(), "enlace"));
            articuloRepository.save(a);
            sincronizarApoyoArticulo(a, docente.getUsuario());
        }
    }

    /** Recurso principal: el registrado como «primario» en recurso_apoyo; en datos anteriores, el primero que no sea complementario. */
    public Recurso recursoPrincipal(MaterialContenido m) {
        Map<Integer, String> tipos = new HashMap<>();
        apoyoRepository.findByMaterial_Id(m.getId()).forEach(a -> { if (a.getRecurso() != null) tipos.put(a.getRecurso().getId(), a.getTipoContenido()); });
        List<Recurso> recursos = recursoRepository.findByMaterial_IdOrderByOrdenAsc(m.getId());
        return recursos.stream().filter(x -> "primario".equals(tipos.get(x.getId()))).findFirst()
                .orElse(recursos.stream().filter(x -> !"complementario".equals(tipos.get(x.getId()))).findFirst().orElse(null));
    }

    /** Mantiene recurso_apoyo al día con el recurso (título, URL, tipo, orden y estado). tipoContenido null = conservar el actual. */
    void sincronizarApoyo(Recurso rec, String tipoContenido, Usuario usuario) {
        RecursoApoyo a = apoyoRepository.findFirstByRecurso_Id(rec.getId())
                .orElseGet(() -> RecursoApoyo.builder().material(rec.getMaterial()).recurso(rec).usuario(usuario).build());
        if (tipoContenido != null) a.setTipoContenido(tipoContenido);
        a.setTipoRecurso("audio".equals(rec.getTipoRecurso()) ? "otro" : rec.getTipoRecurso());
        a.setTitulo(recortar(rec.getNombre(), 250));
        a.setUrl(recortar(rec.getUrl(), 500));
        a.setOrden(Math.min(rec.getOrden() == null ? 1 : rec.getOrden(), 255));
        a.setEstado(rec.getEstado());
        apoyoRepository.save(a);
    }

    /** El artículo científico del material queda en recurso_apoyo como «referencia». */
    void sincronizarApoyoArticulo(ArticuloCientifico art, Usuario usuario) {
        RecursoApoyo a = apoyoRepository.findFirstByArticulo_Id(art.getId())
                .orElseGet(() -> RecursoApoyo.builder().material(art.getMaterial()).articulo(art).usuario(usuario).build());
        a.setTipoContenido("referencia");
        a.setTipoRecurso("enlace");
        a.setTitulo(recortar(art.getTitulo(), 250));
        a.setUrl(recortar(art.getUrl(), 500));
        a.setEstado(art.getEstado());
        apoyoRepository.save(a);
    }

    private static String recortar(String s, int max) { return s == null ? null : (s.length() > max ? s.substring(0, max) : s); }

    /** Código del curso en mayúsculas y único (null si no se envía uno). */
    private String codigoValido(String codigo, Integer idCurso) {
        if (vacio(codigo)) return null;
        String c = codigo.trim().toUpperCase();
        boolean repetido = idCurso == null ? cursoRepository.existsByCodigoIgnoreCase(c) : cursoRepository.existsByCodigoIgnoreCaseAndIdNot(c, idCurso);
        if (repetido) throw new SolicitudInvalidaException("Ya existe un curso con el código " + c);
        return c;
    }

    // ── permisos ────────────────────────────────────────────────────────────

    /** titular, colaborador o invitado (asignación activa), o null si el usuario no pertenece al curso. */
    String rolEnCurso(Curso curso, Usuario usuario) {
        if (usuario.getTipoUsuario() != RolUsuario.docente) return null;
        return perfilDocenteRepository.findByUsuario_Id(usuario.getId())
                .flatMap(d -> cursoDocenteRepository.findByCurso_IdAndDocente_Id(curso.getId(), d.getId()))
                .filter(a -> "activo".equals(a.getEstado()))
                .map(CursoDocente::getRol).orElse(null);
    }

    private void exigirLectura(Curso curso, Usuario usuario) {
        if (usuario.getTipoUsuario() == RolUsuario.admin) return;
        if (rolEnCurso(curso, usuario) == null) throw new SinPermisoException("No eres docente de este curso");
    }

    private void exigirEdicion(Curso curso, Usuario usuario) {
        String rol = rolEnCurso(curso, usuario);
        if (rol == null) {
            throw new SinPermisoException(usuario.getTipoUsuario() == RolUsuario.admin
                    ? "El Administrador consulta el contenido, pero solo los docentes del curso lo editan"
                    : "No eres docente de este curso");
        }
        if ("invitado".equals(rol)) throw new SinPermisoException("Los docentes invitados solo pueden consultar el contenido");
        noArchivado(curso);
    }

    private void noArchivado(Curso curso) {
        if ("archivado".equals(curso.getEstado())) throw new SolicitudInvalidaException("El curso está archivado; no se puede modificar");
    }

    // ── validaciones ────────────────────────────────────────────────────────

    private Leccion prerequisitoValido(Curso curso, Integer idPre, Leccion leccion) {
        Leccion pre = leccionRepository.findById(idPre)
                .orElseThrow(() -> new RecursoNoEncontradoException("La lección prerrequisito no existe"));
        if (!pre.getCurso().getId().equals(curso.getId())) throw new IllegalArgumentException("El prerrequisito debe ser una lección del mismo curso");
        if ("archivada".equals(pre.getEstado())) throw new IllegalArgumentException("El prerrequisito no puede ser una lección archivada");
        if (leccion != null) {
            if (pre.getId().equals(leccion.getId())) throw new IllegalArgumentException("Una lección no puede ser su propio prerrequisito");
            if (pre.getOrden() >= leccion.getOrden()) throw new IllegalArgumentException("El prerrequisito debe ser una lección anterior en el orden");
        }
        return pre;
    }

    /** Video: solo YouTube, convertido al formato para embeber (RNF-015). Otros: http(s) o un archivo subido. */
    static String normalizarUrl(String url, String tipo) {
        String u = url.trim();
        if (u.startsWith("/api/archivos/") && u.contains("?")) u = u.substring(0, u.indexOf('?')); // enlace firmado: se guarda sin firma
        if ("video".equals(tipo)) {
            Matcher m = YOUTUBE.matcher(u);
            if (!m.matches()) throw new IllegalArgumentException("Los videos deben ser de YouTube (youtube.com/watch?v=…, youtu.be/… o youtube.com/shorts/…)");
            return "https://www.youtube.com/embed/" + m.group(1);
        }
        if (!u.matches("^https?://\\S+$") && !u.matches("^/api/archivos/[0-9a-f-]{36}\\.(pdf|png|jpg)$")) {
            throw new IllegalArgumentException("La URL debe empezar por http:// o https://, o ser la de un archivo subido (/api/archivos/…)");
        }
        return u;
    }

    private static String validar(String valor, Set<String> permitidos, String mensaje) {
        String v = valor == null ? "" : valor.trim().toLowerCase();
        if (!permitidos.contains(v)) throw new IllegalArgumentException(mensaje);
        return v;
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }

    private static String texto(String s) { return s == null ? "" : s.trim(); }

    // ── búsquedas ───────────────────────────────────────────────────────────

    private PerfilDocente perfil(Usuario usuario) {
        return perfilDocenteRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los docentes"));
    }

    private Curso buscarCurso(Integer id) {
        return cursoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El curso indicado no existe"));
    }

    private Leccion buscarLeccion(Integer id) {
        return leccionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La lección indicada no existe"));
    }

    private MaterialContenido buscarMaterial(Integer id) {
        return materialRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El material indicado no existe"));
    }

    private Recurso buscarRecurso(Integer id) {
        return recursoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El recurso indicado no existe"));
    }

    // ── conversión ──────────────────────────────────────────────────────────

    CursoDocenteResponse aCursoDocente(Curso c, String rol) {
        PerfilDocente t = c.getDocenteTitular();
        return CursoDocenteResponse.builder()
                .id(c.getId()).nombre(c.getNombre())
                .descripcion(vacio(c.getDescripcion()) ? null : c.getDescripcion())
                .objetivos(vacio(c.getObjetivos()) ? null : c.getObjetivos())
                .imagenPortada(c.getImagenPortada()).precio(c.getPrecio())
                .codigo(c.getCodigo()).color(c.getColor()).infoEstudiante(c.getInfoEstudiante())
                .creditos(c.getCreditos() == null || c.getCreditos() == 0 ? null : c.getCreditos())
                .horasEstimadas(c.getHorasEstimadas()).estado(c.getEstado())
                .idArea(c.getArea().getId()).area(c.getArea().getNombre())
                .idGrado(c.getGrado().getId()).grado(c.getGrado().getNombre())
                .docenteTitular(t != null ? t.getUsuario().getNombre() + " " + t.getUsuario().getApellido() : null)
                .miRol(rol)
                .puedeEditarContenido(!"invitado".equals(rol) && !"archivado".equals(c.getEstado()))
                .totalLecciones(leccionRepository.countByCurso_Id(c.getId()))
                .leccionesActivas(leccionRepository.countByCurso_IdAndEstado(c.getId(), "activa"))
                .pendientesParaPublicar("borrador".equals(c.getEstado()) ? cursoService.pendientesParaPublicar(c) : List.of())
                .build();
    }

    private LeccionResponse.Material aMaterial(MaterialContenido m) {
        List<Recurso> recursos = recursoRepository.findByMaterial_IdOrderByOrdenAsc(m.getId());
        boolean conPrincipal = Set.of("video", "laboratorio", "juego", "documento").contains(m.getTipo());
        Recurso p = conPrincipal ? recursoPrincipal(m) : null;         // recurso principal del material
        ArticuloCientifico a = "articulo".equals(m.getTipo()) ? articuloRepository.findFirstByMaterial_Id(m.getId()).orElse(null) : null;
        return LeccionResponse.Material.builder()
                .id(m.getId()).nombre(m.getNombre()).tipo(m.getTipo()).descripcion(m.getDescripcion())
                .orden(m.getOrden()).estado(m.getEstado())
                .creadoPor(m.getDocente().getUsuario().getNombre() + " " + m.getDocente().getUsuario().getApellido())
                .contenido(m.getContenido()).colorFondo(m.getColorFondo()).guia(m.getGuia())
                .url(a != null ? a.getUrl() : (p != null && "activo".equals(p.getEstado()) ? archivoFirma.firmar(p.getUrl()) : null))
                .proveedor(p != null ? p.getDescripcion() : null)
                .duracionMin(p != null ? p.getDuracionMin() : null)
                .autores(a != null ? a.getAutores() : null).revista(a != null ? a.getRevista() : null).doi(a != null ? a.getDoi() : null)
                .recursos(recursos.stream().map(x -> LeccionResponse.RecursoItem.builder()
                        .id(x.getId()).nombre(x.getNombre()).descripcion(x.getDescripcion()).url(archivoFirma.firmar(x.getUrl()))
                        .tipoRecurso(x.getTipoRecurso()).duracionMin(x.getDuracionMin()).nivelAcademico(x.getNivelAcademico())
                        .tipoContenido(p != null && x.getId().equals(p.getId()) ? "primario" : "complementario")
                        .orden(x.getOrden()).estado(x.getEstado()).build()).toList())
                .build();
    }

    /** Público para que el servicio de inscripciones (Fase 3) reutilice la misma conversión. */
    public LeccionResponse aLeccion(Leccion l, boolean conContenido) {
        List<LeccionResponse.Material> materiales = !conContenido ? List.of() :
                materialRepository.findByLeccion_IdOrderByOrdenAsc(l.getId()).stream().map(this::aMaterial).toList();
        return LeccionResponse.builder()
                .id(l.getId()).idCurso(l.getCurso().getId()).curso(l.getCurso().getNombre())
                .nombre(l.getNombre()).descripcion(l.getDescripcion()).tipo(l.getTipo()).estado(l.getEstado())
                .orden(l.getOrden()).imagenPortada(l.getImagenPortada())
                .idPrerequisito(l.getPrerequisito() != null ? l.getPrerequisito().getId() : null)
                .prerequisito(l.getPrerequisito() != null ? l.getPrerequisito().getNombre() : null)
                .totalMateriales(materialRepository.countByLeccion_Id(l.getId()))
                .materialesActivos(materialRepository.countByLeccion_IdAndEstado(l.getId(), "activo"))
                .materiales(materiales)
                .build();
    }
}
