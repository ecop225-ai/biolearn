package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.InscripcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Inscripciones y aprendizaje del estudiante (Fase 3).
 * - Una inscripción en_reembolso (Pagos, Fase 2) queda en pausa: sin acceso hasta que se resuelva la solicitud.
 * - Solo cursos activos. Gratuito → inscripción activa; con precio → pendiente_pago (la activa el pago aprobado, activarPorPago).
 * - El código de inscripción de una contratación solo lo usan estudiantes de esa institución y vincula la inscripción.
 * - Retirarse y volver a inscribirse reactiva la misma inscripción (se conserva el progreso).
 * - Solo una inscripción activa (o completada) da acceso a las lecciones, materiales y recursos activos.
 * - Una lección con prerrequisito queda bloqueada hasta completarlo.
 * - Progreso: materiales vistos / materiales activos; 100 % completa la lección; todas las lecciones activas
 *   completas completan el curso.
 * - Acceso por tiempo (migración 011): el plazo para terminar empieza cuando el curso se activa (pago aprobado o
 *   inscripción gratuita) y dura curso.meses_para_terminar. Pasado sin terminar, la inscripción queda «vencida»
 *   (sin acceso, conserva el avance) y se renueva pagando de nuevo. Al completar el curso queda congelado en
 *   repaso (solo lectura, el avance no cambia) durante curso.meses_repaso; después queda «cerrada».
 *   El estado se calcula con las fechas en cada consulta (estadoVigente) y la revisión diaria lo guarda.
 */
@Service
@RequiredArgsConstructor
public class InscripcionServiceImpl implements InscripcionService {

    static final Set<String> CON_ACCESO = Set.of("activa", "completada");
    static final java.time.format.DateTimeFormatter FECHA = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ProgresoLeccionRepository progresoLeccionRepository;
    private final ProgresoMaterialRepository progresoMaterialRepository;
    private final LeccionRepository leccionRepository;
    private final MaterialContenidoRepository materialRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final PerfilDocenteRepository perfilDocenteRepository;
    private final CursoDocenteRepository cursoDocenteRepository;
    private final InstitucionCursoContratadoRepository contratacionRepository;
    private final ContenidoCursoServiceImpl contenidoService;
    private final PagoInscripcionRepository pagoInscripcionRepository;

    // ── catálogo e inscripción ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PerfilEstudianteResponse perfil(Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        return PerfilEstudianteResponse.builder()
                .idEstudiante(est.getId()).nombre(usuario.getNombre() + " " + usuario.getApellido()).correo(usuario.getCorreo())
                .idInstitucion(est.getInstitucion() != null ? est.getInstitucion().getId() : null)
                .institucion(est.getInstitucion() != null ? est.getInstitucion().getNombre() : null)
                .grado(est.getGrado() != null ? est.getGrado().getNombre() : null)
                .area(est.getArea() != null ? est.getArea().getNombre() : null)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CursoEstudianteResponse> catalogo(Usuario usuario, String busqueda) {
        PerfilEstudiante est = perfilDe(usuario);
        String q = normalizar(busqueda);
        return cursoRepository.findAll().stream()
                .filter(c -> "activo".equals(c.getEstado()))
                .filter(c -> !leccionesActivas(c.getId()).isEmpty())                 // sin lecciones no se ofrece
                .filter(c -> !soloInstitucional(c) || deMiColegio(c, est))          // solo para colegios: lo ven sus estudiantes
                .filter(c -> q.isEmpty() || normalizar(c.getNombre() + " " + c.getArea().getNombre() + " " + (c.getCodigo() == null ? "" : c.getCodigo())).contains(q))
                .sorted(Comparator.comparing(Curso::getNombre))
                .map(c -> aCurso(c, est, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CursoEstudianteResponse detalle(Integer idCurso, Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        Curso c = buscarCurso(idCurso);
        boolean inscrito = inscripcionRepository.findByCurso_IdAndEstudiante_Id(idCurso, est.getId()).isPresent();
        boolean ofrecido = "activo".equals(c.getEstado()) && !leccionesActivas(c.getId()).isEmpty() && (!soloInstitucional(c) || deMiColegio(c, est));
        if (!ofrecido && !inscrito) throw new RecursoNoEncontradoException("El curso no existe o no está disponible");
        return aCurso(c, est, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CursoEstudianteResponse buscarPorCodigo(String codigo, Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        InstitucionCursoContratado ct = contratacionValida(codigo, est);
        CursoEstudianteResponse r = aCurso(ct.getCurso(), est, true);
        r.setInstitucionCodigo(ct.getInstitucion().getNombre());
        return r;
    }

    @Override
    @Transactional
    public InscripcionResponse inscribirse(InscripcionRequest req, Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        Curso curso = buscarCurso(req.getIdCurso());
        if (!"activo".equals(curso.getEstado())) throw new SolicitudInvalidaException("El curso no está disponible para inscripción");
        InstitucionCursoContratado ct = null;
        if (req.getCodigo() != null && !req.getCodigo().isBlank()) {
            ct = contratacionValida(req.getCodigo(), est);
            if (!ct.getCurso().getId().equals(curso.getId())) throw new IllegalArgumentException("El código de inscripción no corresponde a este curso");
        }
        if (leccionesActivas(curso.getId()).isEmpty())
            throw new SolicitudInvalidaException("El curso todavía no tiene lecciones disponibles; podrás inscribirte cuando se publiquen");
        String estadoInicial = gratuito(curso) ? "activa" : "pendiente_pago";
        Optional<Inscripcion> previa = inscripcionRepository.findByCurso_IdAndEstudiante_Id(curso.getId(), est.getId());
        if (soloInstitucional(curso) && ct == null && previa.map(Inscripcion::getContratacion).isEmpty())
            throw new SolicitudInvalidaException("Este curso es solo para colegios: inscríbete con el código de inscripción que te dio tu colegio");
        Inscripcion insc;
        if (previa.isPresent()) {
            insc = previa.get();
            switch (estadoVigente(insc, LocalDateTime.now())) {
                case "retirada", "vencida" -> insc.setEstado(estadoInicial);  // se reactiva o se renueva y conserva el progreso
                case "cerrada" -> throw new SolicitudInvalidaException(mensajeCerrada(insc));
                case "suspendida" -> throw new SolicitudInvalidaException("Tu inscripción en este curso está suspendida; comunícate con el administrador");
                case "en_reembolso" -> throw new SolicitudInvalidaException("Tienes una solicitud de reembolso en trámite para este curso");
                default -> throw new SolicitudInvalidaException("Ya estás inscrito en este curso");
            }
            if (ct != null) insc.setContratacion(ct);
        } else {
            insc = Inscripcion.builder().curso(curso).estudiante(est).contratacion(ct).estado(estadoInicial).build();
        }
        if ("activa".equals(insc.getEstado()) && sinPlazoVigente(insc)) iniciarPlazo(insc);   // gratuito: el plazo empieza ya
        inscripcionRepository.save(insc);
        if ("activa".equals(insc.getEstado())) actualizarCompletacion(insc);
        return aInscripcion(insc, false);
    }

    /**
     * Renovar un curso vencido: con precio queda pendiente de pago (aparece en el carrito y el pago aprobado abre un
     * plazo nuevo); gratuito se activa de inmediato con un plazo nuevo. El avance se conserva.
     */
    @Override
    @Transactional
    public InscripcionResponse renovar(Integer idInscripcion, Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        Inscripcion insc = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La inscripción indicada no existe"));
        if (!insc.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Esta inscripción no es tuya");
        String vigente = estadoVigente(insc, LocalDateTime.now());
        if ("cerrada".equals(vigente)) throw new SolicitudInvalidaException(mensajeCerrada(insc));
        if (!"vencida".equals(vigente))
            throw new SolicitudInvalidaException("Solo se renueva un curso cuyo plazo venció (este curso está " + vigente.replace('_', ' ') + ")");
        if (!"activo".equals(insc.getCurso().getEstado())) throw new SolicitudInvalidaException("El curso ya no está disponible para renovar");
        if (gratuito(insc.getCurso())) {
            insc.setEstado("activa");
            iniciarPlazo(insc);
        } else {
            insc.setEstado("pendiente_pago");
        }
        inscripcionRepository.save(insc);
        if ("activa".equals(insc.getEstado())) actualizarCompletacion(insc);
        return aInscripcion(insc, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InscripcionResponse> misInscripciones(Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        return inscripcionRepository.findByEstudiante_IdOrderByFechaInscripcionDesc(est.getId()).stream().map(i -> aInscripcion(i, false)).toList();
    }

    @Override
    @Transactional
    public InscripcionResponse retirarse(Integer idInscripcion, Usuario usuario) {
        PerfilEstudiante est = perfilDe(usuario);
        Inscripcion insc = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La inscripción indicada no existe"));
        if (!insc.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Esta inscripción no es tuya");
        if (pagoInscripcionRepository.existsByInscripcion_IdAndPago_EstadoIn(insc.getId(), List.of("pendiente")))
            throw new SolicitudInvalidaException("Tienes un pago en proceso para este curso; cancélalo o espera su resultado antes de retirarte");
        switch (estadoVigente(insc, LocalDateTime.now())) {
            case "activa", "pendiente_pago" -> insc.setEstado("retirada");
            case "completada", "cerrada" -> throw new SolicitudInvalidaException("Ya completaste este curso; no puedes retirarte");
            case "vencida" -> throw new SolicitudInvalidaException("El plazo de este curso venció; puedes renovarlo cuando quieras");
            case "retirada" -> throw new SolicitudInvalidaException("Ya te retiraste de este curso");
            case "en_reembolso" -> throw new SolicitudInvalidaException("Tienes una solicitud de reembolso en trámite para este curso; cancélala o espera su resultado");
            default -> throw new SolicitudInvalidaException("Tu inscripción está suspendida; comunícate con el administrador");
        }
        return aInscripcion(inscripcionRepository.save(insc), false);
    }

    // ── lecciones y progreso ────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<LeccionEstudianteResponse> lecciones(Integer idCurso, Usuario usuario) {
        Inscripcion insc = inscripcionConAcceso(idCurso, perfilDe(usuario));
        Map<Integer, ProgresoLeccion> progreso = progresoPorLeccion(insc);
        BigDecimal pc = porcentajeCurso(insc, progreso);
        return leccionesActivas(idCurso).stream().map(l -> aLeccion(l, insc, progreso, pc, false)).toList();
    }

    @Override
    @Transactional
    public LeccionEstudianteResponse leccion(Integer idLeccion, Usuario usuario) {
        Leccion l = leccionActiva(idLeccion);
        Inscripcion insc = inscripcionConAcceso(l.getCurso().getId(), perfilDe(usuario));
        Map<Integer, ProgresoLeccion> progreso = progresoPorLeccion(insc);
        exigirDesbloqueada(l, progreso);
        ProgresoLeccion p = progreso.get(l.getId());
        if (p == null && !enRepaso(insc)) {                          // primera vez que la abre: en progreso (en repaso no cambia)
            p = ProgresoLeccion.builder().inscripcion(insc).leccion(l).estado("en_progreso").iniciadoEn(LocalDateTime.now()).build();
            progresoLeccionRepository.save(p);
            progreso.put(l.getId(), p);
        }
        return aLeccion(l, insc, progreso, porcentajeCurso(insc, progreso), true);
    }

    /** El portal la llama al abrir un material: registra la primera apertura y devuelve el tiempo mínimo de revisión. */
    @Override
    @Transactional
    public AperturaMaterialResponse registrarApertura(Integer idMaterial, Usuario usuario) {
        MaterialContenido m = materialDisponible(idMaterial);
        Inscripcion insc = inscripcionDesbloqueada(m, usuario);
        if (enRepaso(insc)) {                                         // repaso: solo lectura, no se registra nada
            boolean visto = progresoMaterialRepository.findByInscripcion_IdAndMaterial_Id(insc.getId(), idMaterial)
                    .map(x -> x.getVistoEn() != null).orElse(false);
            long minimos = segundosMinimos(m);
            return AperturaMaterialResponse.builder().idMaterial(idMaterial).tipo(m.getTipo())
                    .segundosMinimos(minimos).segundosTranscurridos(minimos).visto(visto).modoRepaso(true).build();
        }
        ProgresoMaterial pm = progresoMaterialRepository.findByInscripcion_IdAndMaterial_Id(insc.getId(), idMaterial)
                .orElseGet(() -> progresoMaterialRepository.save(ProgresoMaterial.builder().inscripcion(insc).material(m).abiertoEn(LocalDateTime.now()).build()));
        long transcurridos = Math.max(0, java.time.Duration.between(pm.getAbiertoEn(), LocalDateTime.now()).getSeconds());
        return AperturaMaterialResponse.builder().idMaterial(idMaterial).tipo(m.getTipo())
                .segundosMinimos(segundosMinimos(m)).segundosTranscurridos(transcurridos).visto(pm.getVistoEn() != null).build();
    }

    /**
     * Marca el material como visto cuando el portal detecta que el estudiante cumplió el criterio de su tipo.
     * El servidor exige que el material se haya abierto y que haya pasado su tiempo mínimo (migración 006),
     * para que el progreso no pueda completarse llamando la API sin revisar el contenido. Ya no se puede desmarcar.
     */
    @Override
    @Transactional
    public LeccionEstudianteResponse marcarVisto(Integer idMaterial, Usuario usuario) {
        MaterialContenido m = materialDisponible(idMaterial);
        Inscripcion insc = inscripcionDesbloqueada(m, usuario);
        Leccion l = m.getLeccion();
        if (enRepaso(insc)) {                                         // curso completado y congelado: el avance no cambia
            Map<Integer, ProgresoLeccion> progreso = progresoPorLeccion(insc);
            return aLeccion(l, insc, progreso, porcentajeCurso(insc, progreso), true);
        }
        ProgresoMaterial pm = progresoMaterialRepository.findByInscripcion_IdAndMaterial_Id(insc.getId(), idMaterial)
                .orElseThrow(() -> new SolicitudInvalidaException("Abre el material antes de marcarlo como visto"));
        if (pm.getVistoEn() == null) {
            long minimos = segundosMinimos(m);
            long transcurridos = java.time.Duration.between(pm.getAbiertoEn(), LocalDateTime.now()).getSeconds();
            if (transcurridos < minimos)
                throw new SolicitudInvalidaException("Aún no has revisado el material el tiempo suficiente; faltan " + (minimos - transcurridos) + " segundos");
            pm.setVistoEn(LocalDateTime.now());
            progresoMaterialRepository.save(pm);
        }
        Map<Integer, ProgresoLeccion> progreso = progresoPorLeccion(insc);
        ProgresoLeccion p = recalcularLeccion(insc, l, progreso.get(l.getId()));
        progreso.put(l.getId(), p);
        actualizarCompletacion(insc);
        return aLeccion(l, insc, progreso, porcentajeCurso(insc, progreso), true);
    }

    /** Tiempo mínimo de revisión según el tipo: lectura y artículo 20 s, documento 30 s, video la mitad de su duración
     *  (mínimo 30 s), laboratorio y juego la mitad de su duración (mínimo 60 s) o 2 minutos sin duración; tope 20 minutos. */
    long segundosMinimos(MaterialContenido m) {
        Integer dur = null;
        if (Set.of("video", "laboratorio", "juego").contains(m.getTipo())) {
            Recurso principal = contenidoService.recursoPrincipal(m);
            dur = principal != null ? principal.getDuracionMin() : null;
        }
        long s = switch (m.getTipo()) {
            case "documento" -> 30;
            case "video" -> dur != null && dur > 0 ? Math.max(30, dur * 30L) : 30;
            case "laboratorio", "juego" -> dur != null && dur > 0 ? Math.max(60, dur * 30L) : 120;
            default -> 20;                                            // lectura y artículo
        };
        return Math.min(s, SEGUNDOS_MAXIMOS);
    }

    static final long SEGUNDOS_MAXIMOS = 20 * 60;

    private MaterialContenido materialDisponible(Integer idMaterial) {
        MaterialContenido m = materialRepository.findById(idMaterial)
                .orElseThrow(() -> new RecursoNoEncontradoException("El material indicado no existe"));
        if (!"activo".equals(m.getEstado())) throw new RecursoNoEncontradoException("El material no está disponible");
        return m;
    }

    private Inscripcion inscripcionDesbloqueada(MaterialContenido m, Usuario usuario) {
        Leccion l = leccionActiva(m.getLeccion().getId());
        Inscripcion insc = inscripcionConAcceso(l.getCurso().getId(), perfilDe(usuario));
        exigirDesbloqueada(l, progresoPorLeccion(insc));
        return insc;
    }

    /** Porcentaje = materiales vistos / materiales activos; 100 % completa la lección. */
    ProgresoLeccion recalcularLeccion(Inscripcion insc, Leccion l, ProgresoLeccion p) {
        long activos = materialRepository.countByLeccion_IdAndEstado(l.getId(), "activo");
        long vistos = progresoMaterialRepository.contarVistosActivos(insc.getId(), l.getId());
        if (p == null) p = ProgresoLeccion.builder().inscripcion(insc).leccion(l).iniciadoEn(LocalDateTime.now()).build();
        BigDecimal pct = activos == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(vistos * 100.0 / activos).setScale(2, RoundingMode.HALF_UP);
        p.setPorcentaje(pct);
        if (activos > 0 && vistos >= activos) {
            if (!"completada".equals(p.getEstado())) p.setCompletadoEn(LocalDateTime.now());
            p.setEstado("completada");
        } else {
            p.setEstado("en_progreso");
            p.setCompletadoEn(null);
        }
        return progresoLeccionRepository.save(p);
    }

    /**
     * Módulo de Pagos: el pago aprobado activa la inscripción pendiente. Si el estudiante ya había completado
     * todas las lecciones (por ejemplo, antes de retirarse), queda completada.
     */
    public void activarPorPago(Inscripcion insc) {
        if (!"pendiente_pago".equals(insc.getEstado())) return;
        insc.setEstado("activa");
        iniciarPlazo(insc);                                           // cada pago aprobado abre un plazo completo
        inscripcionRepository.save(insc);
        actualizarCompletacion(insc);
    }

    /** Pagos, Fase 2: avance del curso (0 a 100) para la regla de reembolso (menos del 20 %). */
    @Transactional(readOnly = true)
    public BigDecimal avance(Inscripcion insc) {
        return porcentajeCurso(insc, progresoPorLeccion(insc));
    }

    /**
     * El curso se completa cuando todas sus lecciones activas están completadas. Un curso completado queda congelado:
     * si después se agregan lecciones no vuelve a activo (acceso por tiempo). Al completarlo empieza el repaso.
     */
    void actualizarCompletacion(Inscripcion insc) {
        if (!"activa".equals(insc.getEstado())) return;
        List<Leccion> activas = leccionesActivas(insc.getCurso().getId());
        Map<Integer, ProgresoLeccion> progreso = progresoPorLeccion(insc);
        boolean todas = !activas.isEmpty() && activas.stream().allMatch(l -> progreso.containsKey(l.getId()) && "completada".equals(progreso.get(l.getId()).getEstado()));
        if (!todas) return;
        insc.setEstado("completada");
        if (insc.getFechaCompletacion() == null) insc.setFechaCompletacion(LocalDateTime.now());
        if (insc.getRepasoHasta() == null) {
            insc.setRepasoHasta(insc.getFechaCompletacion().plusMonths(mesesRepaso(insc.getCurso())));
            insc.setAvisoRepasoEn(null);
        }
        inscripcionRepository.save(insc);
    }

    // ── acceso por tiempo ───────────────────────────────────────────────────

    /** Estado según las fechas, sin guardarlo: activa con el plazo vencido → vencida; completada con el repaso terminado → cerrada. */
    public static String estadoVigente(Inscripcion i, LocalDateTime ahora) {
        if ("activa".equals(i.getEstado()) && i.getAccesoHasta() != null && ahora.isAfter(i.getAccesoHasta())) return "vencida";
        if ("completada".equals(i.getEstado()) && i.getRepasoHasta() != null && ahora.isAfter(i.getRepasoHasta())) return "cerrada";
        return i.getEstado();
    }

    static boolean enRepaso(Inscripcion i) { return "completada".equals(estadoVigente(i, LocalDateTime.now())); }

    /** Abre un plazo nuevo para terminar el curso (pago aprobado, inscripción gratuita o renovación). */
    void iniciarPlazo(Inscripcion insc) {
        LocalDateTime ahora = LocalDateTime.now();
        insc.setAccesoDesde(ahora);
        insc.setAccesoHasta(ahora.plusMonths(mesesParaTerminar(insc.getCurso())));
        insc.setAvisoVencimientoEn(null);
    }

    private static boolean sinPlazoVigente(Inscripcion i) {
        return i.getAccesoHasta() == null || LocalDateTime.now().isAfter(i.getAccesoHasta());
    }

    static int mesesParaTerminar(Curso c) { return c.getMesesParaTerminar() == null ? 12 : c.getMesesParaTerminar(); }
    static int mesesRepaso(Curso c) { return c.getMesesRepaso() == null ? 6 : c.getMesesRepaso(); }

    private static String mensajeCerrada(Inscripcion i) {
        return "El repaso de este curso terminó" + (i.getRepasoHasta() != null ? " el " + i.getRepasoHasta().format(FECHA) : "")
                + "; conservas tu certificado y tus notas";
    }

    /** Días que faltan para la fecha límite que aplica (terminar o repaso); null si no hay plazo corriendo. */
    static Long diasRestantes(Inscripcion i, String vigente, LocalDateTime ahora) {
        LocalDateTime limite = "activa".equals(vigente) ? i.getAccesoHasta() : "completada".equals(vigente) ? i.getRepasoHasta() : null;
        if (limite == null) return null;
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(ahora.toLocalDate(), limite.toLocalDate()));
    }

    // ── docente y Administrador ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<InscripcionResponse> inscritos(Integer idCurso, Usuario usuario) {
        Curso curso = buscarCurso(idCurso);
        if (usuario.getTipoUsuario() != RolUsuario.admin) {
            boolean esDocente = perfilDocenteRepository.findByUsuario_Id(usuario.getId())
                    .flatMap(d -> cursoDocenteRepository.findByCurso_IdAndDocente_Id(curso.getId(), d.getId()))
                    .filter(a -> "activo".equals(a.getEstado())).isPresent();
            if (!esDocente) throw new SinPermisoException("No eres docente de este curso");
        }
        return inscripcionRepository.findByCurso_IdOrderByFechaInscripcionAsc(idCurso).stream().map(i -> aInscripcion(i, true)).toList();
    }

    @Override
    @Transactional
    public InscripcionResponse cambiarEstado(Integer idInscripcion, CambiarEstadoRequest req) {
        Inscripcion insc = inscripcionRepository.findById(idInscripcion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La inscripción indicada no existe"));
        String nuevo = req.getEstado() == null ? "" : req.getEstado().trim().toLowerCase();
        if (!Set.of("suspendida", "activa").contains(nuevo)) throw new IllegalArgumentException("Estado no válido. El Administrador puede usar: suspendida o activa");
        if (nuevo.equals(insc.getEstado())) throw new SolicitudInvalidaException("La inscripción ya está en estado " + nuevo);
        if ("suspendida".equals(nuevo) && !Set.of("activa", "completada", "pendiente_pago").contains(insc.getEstado()))
            throw new SolicitudInvalidaException("Solo se suspende una inscripción activa, completada o pendiente de pago");
        if ("activa".equals(nuevo) && !"suspendida".equals(insc.getEstado()))
            throw new SolicitudInvalidaException("Solo se reactiva una inscripción suspendida");
        // Reactivar devuelve el estado que tenía: completada si ya lo había terminado (conserva sus fechas)
        insc.setEstado("activa".equals(nuevo) && insc.getFechaCompletacion() != null ? "completada" : nuevo);
        inscripcionRepository.save(insc);
        if ("activa".equals(insc.getEstado())) actualizarCompletacion(insc);
        return aInscripcion(insc, true);
    }

    // ── reglas auxiliares ───────────────────────────────────────────────────

    private static boolean soloInstitucional(Curso c) { return Boolean.TRUE.equals(c.getSoloInstitucional()); }

    /** El colegio del estudiante contrató el curso. */
    private boolean deMiColegio(Curso c, PerfilEstudiante est) {
        return est.getInstitucion() != null && contratacionRepository.existsByCurso_IdAndInstitucion_Id(c.getId(), est.getInstitucion().getId());
    }

    private InstitucionCursoContratado contratacionValida(String codigo, PerfilEstudiante est) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase();
        InstitucionCursoContratado ct = contratacionRepository.findByCodigoInscripcion(c)
                .orElseThrow(() -> new RecursoNoEncontradoException("El código de inscripción no existe"));
        if (est.getInstitucion() == null || !est.getInstitucion().getId().equals(ct.getInstitucion().getId()))
            throw new SinPermisoException("El código pertenece a otra institución; solo lo usan los estudiantes de " + ct.getInstitucion().getNombre());
        if (!"activo".equals(ct.getCurso().getEstado())) throw new SolicitudInvalidaException("El curso de este código no está disponible para inscripción");
        return ct;
    }

    private Inscripcion inscripcionConAcceso(Integer idCurso, PerfilEstudiante est) {
        Inscripcion insc = inscripcionRepository.findByCurso_IdAndEstudiante_Id(idCurso, est.getId())
                .orElseThrow(() -> new SinPermisoException("No estás inscrito en este curso"));
        String vigente = estadoVigente(insc, LocalDateTime.now());
        if (CON_ACCESO.contains(vigente)) return insc;
        throw new SinPermisoException(switch (vigente) {
            case "vencida" -> "El plazo para terminar este curso venció" + (insc.getAccesoHasta() != null ? " el " + insc.getAccesoHasta().format(FECHA) : "")
                    + "; renuévalo para seguir (conservas tu avance)";
            case "cerrada" -> mensajeCerrada(insc);
            case "pendiente_pago" -> insc.getAccesoDesde() != null
                    ? "Tu renovación está pendiente de pago; podrás ver las lecciones cuando se confirme"
                    : "Tu inscripción está pendiente de pago; podrás ver las lecciones cuando se confirme";
            case "suspendida" -> "Tu inscripción está suspendida; comunícate con el administrador";
            case "en_reembolso" -> "Tienes una solicitud de reembolso en trámite para este curso; el acceso vuelve si se rechaza o la cancelas";
            default -> "Te retiraste de este curso; vuelve a inscribirte para ver las lecciones";
        });
    }

    private void exigirDesbloqueada(Leccion l, Map<Integer, ProgresoLeccion> progreso) {
        if (bloqueada(l, progreso)) throw new SolicitudInvalidaException("Completa primero la lección «" + l.getPrerequisito().getNombre() + "»");
    }

    static boolean bloqueada(Leccion l, Map<Integer, ProgresoLeccion> progreso) {
        Leccion pre = l.getPrerequisito();
        if (pre == null || !"activa".equals(pre.getEstado())) return false;     // un prerrequisito inactivo no bloquea
        ProgresoLeccion p = progreso.get(pre.getId());
        return p == null || !"completada".equals(p.getEstado());
    }

    private Map<Integer, ProgresoLeccion> progresoPorLeccion(Inscripcion insc) {
        Map<Integer, ProgresoLeccion> m = new HashMap<>();
        progresoLeccionRepository.findByInscripcion_Id(insc.getId()).forEach(p -> m.put(p.getLeccion().getId(), p));
        return m;
    }

    private List<Leccion> leccionesActivas(Integer idCurso) {
        return leccionRepository.findByCurso_IdOrderByOrdenAsc(idCurso).stream().filter(l -> "activa".equals(l.getEstado())).toList();
    }

    /** Promedio del porcentaje de las lecciones activas. */
    private BigDecimal porcentajeCurso(Inscripcion insc, Map<Integer, ProgresoLeccion> progreso) {
        List<Leccion> activas = leccionesActivas(insc.getCurso().getId());
        if (activas.isEmpty()) return BigDecimal.ZERO;
        BigDecimal suma = activas.stream().map(l -> progreso.containsKey(l.getId()) ? progreso.get(l.getId()).getPorcentaje() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(activas.size()), 2, RoundingMode.HALF_UP);
    }

    private Leccion leccionActiva(Integer id) {
        Leccion l = leccionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La lección indicada no existe"));
        if (!"activa".equals(l.getEstado())) throw new RecursoNoEncontradoException("La lección no está disponible");
        return l;
    }

    private PerfilEstudiante perfilDe(Usuario usuario) {
        return perfilEstudianteRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los estudiantes"));
    }

    private Curso buscarCurso(Integer id) {
        return cursoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El curso indicado no existe"));
    }

    private static boolean gratuito(Curso c) { return c.getPrecio() == null || c.getPrecio().compareTo(BigDecimal.ZERO) == 0; }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    // ── conversión ──────────────────────────────────────────────────────────

    private CursoEstudianteResponse aCurso(Curso c, PerfilEstudiante est, boolean conLecciones) {
        Optional<Inscripcion> insc = inscripcionRepository.findByCurso_IdAndEstudiante_Id(c.getId(), est.getId());
        boolean deMiColegio = deMiColegio(c, est);
        List<Leccion> activas = leccionesActivas(c.getId());
        PerfilDocente t = c.getDocenteTitular();
        return CursoEstudianteResponse.builder()
                .id(c.getId()).nombre(c.getNombre()).codigo(c.getCodigo()).color(c.getColor()).imagenPortada(c.getImagenPortada())
                .descripcion(c.getDescripcion()).objetivos(c.getObjetivos()).infoEstudiante(conLecciones ? c.getInfoEstudiante() : null)
                .area(c.getArea().getNombre()).grado(c.getGrado().getNombre())
                .precio(c.getPrecio()).gratuito(gratuito(c))
                .creditos(c.getCreditos() == null || c.getCreditos() == 0 ? null : c.getCreditos()).horasEstimadas(c.getHorasEstimadas())
                .docenteTitular(t != null ? t.getUsuario().getNombre() + " " + t.getUsuario().getApellido() : null)
                .totalLecciones(activas.size()).deMiColegio(deMiColegio).soloInstitucional(soloInstitucional(c))
                .idInscripcion(insc.map(Inscripcion::getId).orElse(null))
                .estadoInscripcion(insc.map(i -> estadoVigente(i, LocalDateTime.now())).orElse(null))
                .porcentaje(insc.filter(i -> CON_ACCESO.contains(i.getEstado())).map(i -> porcentajeCurso(i, progresoPorLeccion(i))).orElse(null))
                .mesesParaTerminar(mesesParaTerminar(c)).mesesRepaso(mesesRepaso(c))
                .lecciones(conLecciones ? activas.stream().map(Leccion::getNombre).toList() : null)
                .build();
    }

    private InscripcionResponse aInscripcion(Inscripcion i, boolean conEstudiante) {
        Curso c = i.getCurso();
        Map<Integer, ProgresoLeccion> progreso = progresoPorLeccion(i);
        List<Leccion> activas = leccionesActivas(c.getId());
        long completadas = activas.stream().filter(l -> progreso.containsKey(l.getId()) && "completada".equals(progreso.get(l.getId()).getEstado())).count();
        Usuario u = i.getEstudiante().getUsuario();
        LocalDateTime ahora = LocalDateTime.now();
        String vigente = estadoVigente(i, ahora);
        return InscripcionResponse.builder()
                .id(i.getId()).idCurso(c.getId()).curso(c.getNombre()).codigoCurso(c.getCodigo()).color(c.getColor()).imagenPortada(c.getImagenPortada())
                .precio(c.getPrecio()).estado(vigente).fechaInscripcion(i.getFechaInscripcion()).fechaCompletacion(i.getFechaCompletacion())
                .accesoDesde(i.getAccesoDesde()).accesoHasta(i.getAccesoHasta()).repasoHasta(i.getRepasoHasta())
                .diasRestantes(diasRestantes(i, vigente, ahora)).modoRepaso("completada".equals(vigente))
                .renovacion("pendiente_pago".equals(vigente) && i.getAccesoDesde() != null)
                .mesesParaTerminar(mesesParaTerminar(c)).mesesRepaso(mesesRepaso(c))
                .institucion(i.getContratacion() != null ? i.getContratacion().getInstitucion().getNombre() : null)
                .porcentaje(porcentajeCurso(i, progreso)).leccionesCompletadas(completadas).totalLecciones(activas.size())
                .idEstudiante(conEstudiante ? i.getEstudiante().getId() : null)
                .estudiante(conEstudiante ? u.getNombre() + " " + u.getApellido() : null)
                .correo(conEstudiante ? u.getCorreo() : null)
                .build();
    }

    private LeccionEstudianteResponse aLeccion(Leccion l, Inscripcion insc, Map<Integer, ProgresoLeccion> progreso, BigDecimal pc, boolean conContenido) {
        ProgresoLeccion p = progreso.get(l.getId());
        long activos = materialRepository.countByLeccion_IdAndEstado(l.getId(), "activo");
        long vistos = progresoMaterialRepository.contarVistosActivos(insc.getId(), l.getId());
        List<LeccionEstudianteResponse.MaterialEstudiante> materiales = null;
        if (conContenido) {
            Set<Integer> idsVistos = new HashSet<>(progresoMaterialRepository.idsVistos(insc.getId()));
            Map<Integer, Long> minimos = new HashMap<>();
            materialRepository.findByLeccion_IdOrderByOrdenAsc(l.getId()).forEach(x -> minimos.put(x.getId(), segundosMinimos(x)));
            materiales = contenidoService.aLeccion(l, true).getMateriales().stream()
                    .filter(m -> "activo".equals(m.getEstado()))
                    .peek(m -> m.setRecursos(m.getRecursos().stream().filter(r -> "activo".equals(r.getEstado())).toList()))
                    .map(m -> LeccionEstudianteResponse.MaterialEstudiante.builder().visto(idsVistos.contains(m.getId()))
                            .segundosMinimos(minimos.getOrDefault(m.getId(), 20L)).material(m).build())
                    .toList();
        }
        return LeccionEstudianteResponse.builder()
                .id(l.getId()).idCurso(l.getCurso().getId()).curso(l.getCurso().getNombre())
                .nombre(l.getNombre()).descripcion(l.getDescripcion()).tipo(l.getTipo()).orden(l.getOrden()).imagenPortada(l.getImagenPortada())
                .idPrerequisito(l.getPrerequisito() != null ? l.getPrerequisito().getId() : null)
                .prerequisito(l.getPrerequisito() != null ? l.getPrerequisito().getNombre() : null)
                .bloqueada(bloqueada(l, progreso))
                .estadoProgreso(p != null ? p.getEstado() : "pendiente")
                .porcentaje(p != null ? p.getPorcentaje() : BigDecimal.ZERO)
                .materialesVistos(vistos).totalMateriales(activos)
                .porcentajeCurso(pc).estadoInscripcion(estadoVigente(insc, LocalDateTime.now())).modoRepaso(enRepaso(insc))
                .repasoHasta(insc.getRepasoHasta())
                .materiales(materiales)
                .build();
    }
}
