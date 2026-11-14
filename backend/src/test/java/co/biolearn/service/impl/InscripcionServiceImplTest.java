package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InscripcionServiceImpl — inscripciones y aprendizaje del estudiante (Fase 3)")
class InscripcionServiceImplTest {

    @Mock private CursoRepository cursoRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private ProgresoLeccionRepository progresoLeccionRepository;
    @Mock private ProgresoMaterialRepository progresoMaterialRepository;
    @Mock private LeccionRepository leccionRepository;
    @Mock private MaterialContenidoRepository materialRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private PerfilDocenteRepository perfilDocenteRepository;
    @Mock private CursoDocenteRepository cursoDocenteRepository;
    @Mock private InstitucionCursoContratadoRepository contratacionRepository;
    @Mock private ContenidoCursoServiceImpl contenidoService;
    @Mock private PagoInscripcionRepository pagoInscripcionRepository;

    @InjectMocks private InscripcionServiceImpl servicio;

    private Usuario estU, otroU, adminU, docU;
    private PerfilEstudiante est;
    private Institucion colegio, otroColegio;
    private Curso gratis, pago;
    private final List<ProgresoLeccion> progresos = new ArrayList<>();

    private static Usuario usuario(int id, RolUsuario rol) {
        return Usuario.builder().id(id).nombre("N" + id).apellido("A").correo(id + "@t.co").tipoUsuario(rol).estado(EstadoUsuario.activo).build();
    }

    private Curso curso(int id, String estado, BigDecimal precio) {
        Curso c = Curso.builder().id(id).nombre("Curso " + id).estado(estado).precio(precio)
                .area(AreaAcademica.builder().id(1).nombre("Biología").estado("activo").build())
                .grado(Grado.builder().id(1).nombre("Noveno").nivel("secundaria").orden(9).estado("activo").build()).build();
        when(cursoRepository.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    @BeforeEach
    void datos() {
        estU = usuario(30, RolUsuario.estudiante); otroU = usuario(31, RolUsuario.estudiante);
        adminU = usuario(1, RolUsuario.admin); docU = usuario(20, RolUsuario.docente);
        colegio = Institucion.builder().id(4).nombre("Colegio A").estado("activa").build();
        otroColegio = Institucion.builder().id(5).nombre("Colegio B").estado("activa").build();
        est = PerfilEstudiante.builder().id(9).usuario(estU).institucion(colegio).build();
        when(perfilEstudianteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(est));
        when(perfilEstudianteRepository.findByUsuario_Id(31)).thenReturn(Optional.of(PerfilEstudiante.builder().id(10).usuario(otroU).build()));
        gratis = curso(10, "activo", BigDecimal.ZERO);
        pago = curso(11, "activo", new BigDecimal("95000"));
        when(inscripcionRepository.findByCurso_IdAndEstudiante_Id(anyInt(), anyInt())).thenReturn(Optional.empty());
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(i -> { Inscripcion x = i.getArgument(0); if (x.getId() == null) x.setId(100); return x; });
        when(progresoLeccionRepository.findByInscripcion_Id(anyInt())).thenAnswer(i -> new ArrayList<>(progresos));
        when(progresoLeccionRepository.save(any(ProgresoLeccion.class))).thenAnswer(i -> { ProgresoLeccion p = i.getArgument(0); progresos.removeIf(x -> x.getLeccion().getId().equals(p.getLeccion().getId())); progresos.add(p); return p; });
        when(leccionRepository.findByCurso_IdOrderByOrdenAsc(anyInt())).thenReturn(List.of());
    }

    private Inscripcion inscripcion(Curso c, String estado) {
        Inscripcion i = Inscripcion.builder().id(100).curso(c).estudiante(est).estado(estado).build();
        when(inscripcionRepository.findByCurso_IdAndEstudiante_Id(c.getId(), 9)).thenReturn(Optional.of(i));
        when(inscripcionRepository.findById(100)).thenReturn(Optional.of(i));
        return i;
    }

    private Leccion leccion(int id, Curso c, String estado, Leccion pre) {
        Leccion l = Leccion.builder().id(id).curso(c).nombre("Lección " + id).orden(id).estado(estado).prerequisito(pre).build();
        when(leccionRepository.findById(id)).thenReturn(Optional.of(l));
        return l;
    }

    private InstitucionCursoContratado contratacion(Curso c, Institucion inst, String codigo) {
        InstitucionCursoContratado ct = InstitucionCursoContratado.builder().id(7).curso(c).institucion(inst).codigoInscripcion(codigo).build();
        when(contratacionRepository.findByCodigoInscripcion(codigo)).thenReturn(Optional.of(ct));
        return ct;
    }

    // ── inscripción ──
    @Nested @DisplayName("Inscripción")
    class InscripcionTests {

        @Test @DisplayName("Curso gratuito → inscripción activa")
        void gratuito() {
            assertThat(servicio.inscribirse(new InscripcionRequest(10, null), estU).getEstado()).isEqualTo("activa");
        }

        @Test @DisplayName("Curso con precio → pendiente de pago")
        void conPrecio() {
            assertThat(servicio.inscribirse(new InscripcionRequest(11, null), estU).getEstado()).isEqualTo("pendiente_pago");
        }

        @Test @DisplayName("Curso no activo → 409")
        void noActivo() {
            curso(12, "borrador", BigDecimal.ZERO);
            assertThatThrownBy(() -> servicio.inscribirse(new InscripcionRequest(12, null), estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El curso no está disponible para inscripción");
        }

        @Test @DisplayName("Inscribirse dos veces → 409")
        void repetida() {
            inscripcion(gratis, "activa");
            assertThatThrownBy(() -> servicio.inscribirse(new InscripcionRequest(10, null), estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Ya estás inscrito en este curso");
        }

        @Test @DisplayName("Volver tras retirarse → se reactiva la misma inscripción")
        void reactivar() {
            Inscripcion i = inscripcion(gratis, "retirada");
            InscripcionResponse r = servicio.inscribirse(new InscripcionRequest(10, null), estU);
            assertThat(r.getId()).isEqualTo(100);
            assertThat(i.getEstado()).isEqualTo("activa");
        }

        @Test @DisplayName("Inscripción suspendida → 409")
        void suspendida() {
            inscripcion(gratis, "suspendida");
            assertThatThrownBy(() -> servicio.inscribirse(new InscripcionRequest(10, null), estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("suspendida");
        }

        @Test @DisplayName("Con el código de su colegio → queda vinculada a la contratación")
        void conCodigo() {
            InstitucionCursoContratado ct = contratacion(pago, colegio, "ABCD2345");
            servicio.inscribirse(new InscripcionRequest(11, "abcd2345"), estU);
            verify(inscripcionRepository).save(argThat(i -> i.getContratacion() == ct && "pendiente_pago".equals(i.getEstado())));
        }

        @Test @DisplayName("Código de otra institución → 403")
        void codigoOtroColegio() {
            contratacion(pago, otroColegio, "ZZZZ9999");
            assertThatThrownBy(() -> servicio.inscribirse(new InscripcionRequest(11, "ZZZZ9999"), estU))
                    .isInstanceOf(SinPermisoException.class).hasMessageContaining("otra institución");
        }

        @Test @DisplayName("Estudiante sin institución usa un código → 403")
        void sinInstitucion() {
            contratacion(pago, colegio, "ABCD2345");
            assertThatThrownBy(() -> servicio.buscarPorCodigo("ABCD2345", otroU)).isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("Código inexistente → 404; código de otro curso → 400")
        void codigoInvalido() {
            when(contratacionRepository.findByCodigoInscripcion("NOEXISTE")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.buscarPorCodigo("NOEXISTE", estU)).isInstanceOf(RecursoNoEncontradoException.class);
            contratacion(pago, colegio, "ABCD2345");
            assertThatThrownBy(() -> servicio.inscribirse(new InscripcionRequest(10, "ABCD2345"), estU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no corresponde a este curso");
        }

        @Test @DisplayName("Perfil: devuelve la institución del estudiante")
        void perfil() {
            PerfilEstudianteResponse r = servicio.perfil(estU);
            assertThat(r.getIdInstitucion()).isEqualTo(4);
            assertThat(r.getInstitucion()).isEqualTo("Colegio A");
        }

        @Test @DisplayName("Un usuario sin perfil de estudiante → 403")
        void sinPerfil() {
            when(perfilEstudianteRepository.findByUsuario_Id(1)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.misInscripciones(adminU)).isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("Catálogo: solo cursos activos y marca «de mi colegio»")
        void catalogo() {
            Curso borrador = curso(12, "borrador", BigDecimal.ZERO);
            when(cursoRepository.findAll()).thenReturn(List.of(gratis, pago, borrador));
            when(contratacionRepository.existsByCurso_IdAndInstitucion_Id(11, 4)).thenReturn(true);
            List<CursoEstudianteResponse> r = servicio.catalogo(estU, null);
            assertThat(r).extracting(CursoEstudianteResponse::getId).containsExactly(10, 11);
            assertThat(r.get(1).isDeMiColegio()).isTrue();
            assertThat(r.get(0).isGratuito()).isTrue();
        }
    }

    // ── retiro y gestión ──
    @Nested @DisplayName("Retiro y gestión del Administrador")
    class Gestion {

        @Test @DisplayName("Retirarse de una inscripción activa → retirada")
        void retirarse() {
            inscripcion(gratis, "activa");
            assertThat(servicio.retirarse(100, estU).getEstado()).isEqualTo("retirada");
        }

        @Test @DisplayName("Retirarse de un curso completado → 409; de una inscripción ajena → 403")
        void retiroInvalido() {
            inscripcion(gratis, "completada");
            assertThatThrownBy(() -> servicio.retirarse(100, estU)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("Ya completaste");
            assertThatThrownBy(() -> servicio.retirarse(100, otroU)).isInstanceOf(SinPermisoException.class).hasMessage("Esta inscripción no es tuya");
        }

        @Test @DisplayName("Pagos: retirarse con un pago en proceso → 409")
        void retiroConPagoPendiente() {
            inscripcion(pago, "pendiente_pago");
            when(pagoInscripcionRepository.existsByInscripcion_IdAndPago_EstadoIn(eq(100), anyCollection())).thenReturn(true);
            assertThatThrownBy(() -> servicio.retirarse(100, estU)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("pago en proceso");
        }

        @Test @DisplayName("Pagos: el pago aprobado activa la inscripción pendiente; otra no cambia")
        void activarPorPago() {
            Inscripcion i = inscripcion(pago, "pendiente_pago");
            servicio.activarPorPago(i);
            assertThat(i.getEstado()).isEqualTo("activa");
            Inscripcion r = Inscripcion.builder().id(101).curso(pago).estudiante(est).estado("retirada").build();
            servicio.activarPorPago(r);
            assertThat(r.getEstado()).isEqualTo("retirada");
        }

        @Test @DisplayName("El Administrador suspende y reactiva")
        void suspenderReactivar() {
            Inscripcion i = inscripcion(gratis, "activa");
            CambiarEstadoRequest s = new CambiarEstadoRequest(); s.setEstado("suspendida");
            servicio.cambiarEstado(100, s);
            assertThat(i.getEstado()).isEqualTo("suspendida");
            CambiarEstadoRequest a = new CambiarEstadoRequest(); a.setEstado("activa");
            servicio.cambiarEstado(100, a);
            assertThat(i.getEstado()).isEqualTo("activa");
        }

        @Test @DisplayName("Reactivar una inscripción que no está suspendida → 409; estado inválido → 400")
        void estadoInvalido() {
            inscripcion(gratis, "retirada");
            CambiarEstadoRequest a = new CambiarEstadoRequest(); a.setEstado("activa");
            assertThatThrownBy(() -> servicio.cambiarEstado(100, a)).isInstanceOf(SolicitudInvalidaException.class).hasMessage("Solo se reactiva una inscripción suspendida");
            CambiarEstadoRequest x = new CambiarEstadoRequest(); x.setEstado("completada");
            assertThatThrownBy(() -> servicio.cambiarEstado(100, x)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("Inscritos: un docente ajeno al curso → 403; el Administrador los ve")
        void inscritos() {
            when(perfilDocenteRepository.findByUsuario_Id(20)).thenReturn(Optional.of(PerfilDocente.builder().id(5).usuario(docU).estado(EstadoPerfilAmplio.activo).build()));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 5)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.inscritos(10, docU)).isInstanceOf(SinPermisoException.class);
            Inscripcion insc = inscripcion(gratis, "activa");
            when(inscripcionRepository.findByCurso_IdOrderByFechaInscripcionAsc(10)).thenReturn(List.of(insc));
            List<InscripcionResponse> r = servicio.inscritos(10, adminU);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getCorreo()).isEqualTo("30@t.co");
        }
    }

    // ── acceso y progreso ──
    @Nested @DisplayName("Acceso a las lecciones y progreso")
    class Progreso {

        @Test @DisplayName("Sin inscripción → 403; pendiente de pago → 403 con el motivo")
        void sinAcceso() {
            assertThatThrownBy(() -> servicio.lecciones(10, estU)).isInstanceOf(SinPermisoException.class).hasMessage("No estás inscrito en este curso");
            inscripcion(pago, "pendiente_pago");
            assertThatThrownBy(() -> servicio.lecciones(11, estU)).isInstanceOf(SinPermisoException.class).hasMessageContaining("pendiente de pago");
        }

        @Test @DisplayName("Solo se listan las lecciones activas; la que tiene prerrequisito pendiente queda bloqueada")
        void bloqueo() {
            inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            Leccion l2 = leccion(2, gratis, "activa", l1);
            Leccion l3 = leccion(3, gratis, "borrador", null);
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(List.of(l1, l2, l3));
            List<LeccionEstudianteResponse> r = servicio.lecciones(10, estU);
            assertThat(r).extracting(LeccionEstudianteResponse::getId).containsExactly(1, 2);
            assertThat(r.get(1).isBloqueada()).isTrue();
        }

        @Test @DisplayName("Abrir una lección bloqueada → 409 con el nombre del prerrequisito")
        void abrirBloqueada() {
            inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            leccion(2, gratis, "activa", l1);
            assertThatThrownBy(() -> servicio.leccion(2, estU)).isInstanceOf(SolicitudInvalidaException.class).hasMessage("Completa primero la lección «Lección 1»");
        }

        @Test @DisplayName("Abrir una lección por primera vez → queda en progreso")
        void abrir() {
            inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            when(contenidoService.aLeccion(l1, true)).thenReturn(LeccionResponse.builder().materiales(List.of()).build());
            LeccionEstudianteResponse r = servicio.leccion(1, estU);
            assertThat(r.getEstadoProgreso()).isEqualTo("en_progreso");
            verify(progresoLeccionRepository).save(argThat(p -> "en_progreso".equals(p.getEstado())));
        }

        @Test @DisplayName("Una lección inactiva no se puede abrir → 404")
        void leccionInactiva() {
            leccion(5, gratis, "inactiva", null);
            assertThatThrownBy(() -> servicio.leccion(5, estU)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        /** Lección con un material de lectura activo (2 materiales activos en total) y su progreso abierto hace «hace» segundos. */
        private ProgresoMaterial preparar(Inscripcion insc, Leccion l1, MaterialContenido m, Long haceSegundos) {
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(List.of(l1));
            when(materialRepository.findById(m.getId())).thenReturn(Optional.of(m));
            when(materialRepository.countByLeccion_IdAndEstado(1, "activo")).thenReturn(2L);
            when(contenidoService.aLeccion(l1, true)).thenReturn(LeccionResponse.builder().materiales(List.of()).build());
            if (haceSegundos == null) {
                when(progresoMaterialRepository.findByInscripcion_IdAndMaterial_Id(100, m.getId())).thenReturn(Optional.empty());
                return null;
            }
            ProgresoMaterial pm = ProgresoMaterial.builder().id(1).inscripcion(insc).material(m)
                    .abiertoEn(java.time.LocalDateTime.now().minusSeconds(haceSegundos)).build();
            when(progresoMaterialRepository.findByInscripcion_IdAndMaterial_Id(100, m.getId())).thenReturn(Optional.of(pm));
            return pm;
        }

        @Test @DisplayName("Abrir un material registra la apertura y devuelve su tiempo mínimo")
        void apertura() {
            Inscripcion insc = inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            MaterialContenido m = MaterialContenido.builder().id(50).leccion(l1).tipo("lectura").estado("activo").build();
            preparar(insc, l1, m, null);
            when(progresoMaterialRepository.save(any(ProgresoMaterial.class))).thenAnswer(i -> i.getArgument(0));
            AperturaMaterialResponse r = servicio.registrarApertura(50, estU);
            assertThat(r.getSegundosMinimos()).isEqualTo(20);
            assertThat(r.isVisto()).isFalse();
            verify(progresoMaterialRepository).save(argThat(pm -> pm.getAbiertoEn() != null && pm.getVistoEn() == null));
        }

        @Test @DisplayName("Marcar como visto sin haber abierto el material → 409")
        void sinApertura() {
            Inscripcion insc = inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            preparar(insc, l1, MaterialContenido.builder().id(50).leccion(l1).tipo("lectura").estado("activo").build(), null);
            assertThatThrownBy(() -> servicio.marcarVisto(50, estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Abre el material antes de marcarlo como visto");
        }

        @Test @DisplayName("Marcar como visto antes del tiempo mínimo → 409 con los segundos que faltan")
        void antesDeTiempo() {
            Inscripcion insc = inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            ProgresoMaterial pm = preparar(insc, l1, MaterialContenido.builder().id(50).leccion(l1).tipo("lectura").estado("activo").build(), 5L);
            assertThatThrownBy(() -> servicio.marcarVisto(50, estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("faltan");
            assertThat(pm.getVistoEn()).isNull();
        }

        @Test @DisplayName("Cumplido el tiempo: 1 de 2 materiales → 50 %; los 2 → lección y curso completados")
        void marcarVisto() {
            Inscripcion insc = inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            ProgresoMaterial pm = preparar(insc, l1, MaterialContenido.builder().id(50).leccion(l1).tipo("lectura").estado("activo").build(), 60L);
            when(progresoMaterialRepository.contarVistosActivos(100, 1)).thenReturn(1L);
            LeccionEstudianteResponse r = servicio.marcarVisto(50, estU);
            assertThat(pm.getVistoEn()).isNotNull();
            assertThat(r.getPorcentaje()).isEqualByComparingTo("50");
            assertThat(r.getEstadoProgreso()).isEqualTo("en_progreso");
            when(progresoMaterialRepository.contarVistosActivos(100, 1)).thenReturn(2L);
            r = servicio.marcarVisto(50, estU);
            assertThat(r.getEstadoProgreso()).isEqualTo("completada");
            assertThat(insc.getEstado()).isEqualTo("completada");
            assertThat(insc.getFechaCompletacion()).isNotNull();
        }

        @Test @DisplayName("Marcar dos veces no cambia la fecha en que se vio")
        void marcarDosVeces() {
            Inscripcion insc = inscripcion(gratis, "activa");
            Leccion l1 = leccion(1, gratis, "activa", null);
            ProgresoMaterial pm = preparar(insc, l1, MaterialContenido.builder().id(50).leccion(l1).tipo("lectura").estado("activo").build(), 60L);
            java.time.LocalDateTime antes = java.time.LocalDateTime.now().minusSeconds(30);
            pm.setVistoEn(antes);
            servicio.marcarVisto(50, estU);
            assertThat(pm.getVistoEn()).isEqualTo(antes);
        }

        @Test @DisplayName("Tiempo mínimo según el tipo de material")
        void tiemposPorTipo() {
            Leccion l1 = leccion(1, gratis, "activa", null);
            java.util.function.Function<String, MaterialContenido> mat = t -> MaterialContenido.builder().id(60).leccion(l1).tipo(t).build();
            assertThat(servicio.segundosMinimos(mat.apply("lectura"))).isEqualTo(20);
            assertThat(servicio.segundosMinimos(mat.apply("articulo"))).isEqualTo(20);
            assertThat(servicio.segundosMinimos(mat.apply("documento"))).isEqualTo(30);
            assertThat(servicio.segundosMinimos(mat.apply("juego"))).isEqualTo(120);          // sin duración
            when(contenidoService.recursoPrincipal(any())).thenReturn(Recurso.builder().duracionMin(10).build());
            assertThat(servicio.segundosMinimos(mat.apply("video"))).isEqualTo(300);          // la mitad de 10 min
            assertThat(servicio.segundosMinimos(mat.apply("laboratorio"))).isEqualTo(300);
            when(contenidoService.recursoPrincipal(any())).thenReturn(Recurso.builder().duracionMin(90).build());
            assertThat(servicio.segundosMinimos(mat.apply("video"))).isEqualTo(InscripcionServiceImpl.SEGUNDOS_MAXIMOS);  // tope de 20 min
        }

        @Test @DisplayName("Abrir o marcar un material inactivo → 404")
        void materialInactivo() {
            Leccion l1 = leccion(1, gratis, "activa", null);
            when(materialRepository.findById(51)).thenReturn(Optional.of(MaterialContenido.builder().id(51).leccion(l1).estado("inactivo").build()));
            assertThatThrownBy(() -> servicio.marcarVisto(51, estU)).isInstanceOf(RecursoNoEncontradoException.class);
            assertThatThrownBy(() -> servicio.registrarApertura(51, estU)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test @DisplayName("Un prerrequisito inactivo no bloquea la lección")
        void prerequisitoInactivo() {
            Leccion pre = Leccion.builder().id(1).estado("inactiva").nombre("Vieja").build();
            Leccion l = Leccion.builder().id(2).estado("activa").prerequisito(pre).build();
            assertThat(InscripcionServiceImpl.bloqueada(l, Map.of())).isFalse();
        }
    }
}
