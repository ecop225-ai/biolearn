package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CursoServiceImpl — gestión de cursos del Administrador (Fase 1)")
class CursoServiceImplTest {

    @Mock private CursoRepository cursoRepository;
    @Mock private CursoDocenteRepository cursoDocenteRepository;
    @Mock private HorarioCursoRepository horarioRepository;
    @Mock private InstitucionCursoContratadoRepository contratacionRepository;
    @Mock private GradoRepository gradoRepository;
    @Mock private AreaAcademicaRepository areaRepository;
    @Mock private PerfilDocenteRepository perfilDocenteRepository;
    @Mock private PerfilAdminRepository perfilAdminRepository;
    @Mock private InstitucionRepository institucionRepository;
    @Mock private LeccionRepository leccionRepository;
    @Mock private InscripcionRepository inscripcionRepository;

    @InjectMocks private CursoServiceImpl cursoService;

    /** Fase 2: por defecto, cada curso tiene una lección activa (los casos que prueban lo contrario lo sobrescriben). */
    @BeforeEach
    void cursoConLeccionActiva() {
        lenient().when(leccionRepository.countByCurso_IdAndEstado(any(), eq("activa"))).thenReturn(1L);
    }

    // ── datos de prueba ─────────────────────────────────────────────────────

    private static AreaAcademica area(String estado) {
        return AreaAcademica.builder().id(1).nombre("Biología molecular").estado(estado).build();
    }

    private static Grado grado(String estado) {
        return Grado.builder().id(1).nombre("Sexto").nivel("secundaria").orden(6).estado(estado).build();
    }

    private static PerfilDocente docente(int idUsuario, int idPerfil, EstadoUsuario estadoCuenta) {
        Usuario u = Usuario.builder().id(idUsuario).nombre("Julián").apellido("Prada").correo("julian@biolearn.com")
                .estado(estadoCuenta).build();
        return PerfilDocente.builder().id(idPerfil).usuario(u).estado(EstadoPerfilAmplio.activo).build();
    }

    private static Curso curso(String estado) {
        return Curso.builder().id(10).nombre("Genética básica").area(area("activo")).grado(grado("activo"))
                .descripcion("Introducción a la herencia").precio(BigDecimal.ZERO).estado(estado).build();
    }

    private static Curso cursoListo(String estado) {
        Curso c = curso(estado);
        c.setDocenteTitular(docente(20, 5, EstadoUsuario.activo));
        return c;
    }

    private static CambiarEstadoRequest estado(String valor) {
        CambiarEstadoRequest r = new CambiarEstadoRequest();
        r.setEstado(valor);
        return r;
    }

    private void guardarDevuelveLoMismo() {
        when(cursoRepository.save(any(Curso.class))).thenAnswer(i -> {
            Curso c = i.getArgument(0);
            if (c.getId() == null) c.setId(10);
            return c;
        });
    }

    // ── crear ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Crear cursos")
    class Crear {

        private CrearCursoRequest valido() {
            return CrearCursoRequest.builder().nombre("  Genética básica ").idArea(1).idGrado(1)
                    .descripcion("Introducción a la herencia").precio(new BigDecimal("120000")).build();
        }

        @Test
        @DisplayName("Curso válido sin docente → queda en borrador, con el admin creador y precio")
        void crearSinDocente() {
            when(cursoRepository.existsByNombreIgnoreCaseAndEstadoNot("Genética básica", "archivado")).thenReturn(false);
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado("activo")));
            PerfilAdmin admin = PerfilAdmin.builder().id(3).build();
            when(perfilAdminRepository.findByUsuario_Id(7)).thenReturn(Optional.of(admin));
            guardarDevuelveLoMismo();

            CursoResponse r = cursoService.crear(valido(), 7);

            assertThat(r.getEstado()).isEqualTo("borrador");
            assertThat(r.getNombre()).isEqualTo("Genética básica");
            assertThat(r.getPrecio()).isEqualByComparingTo("120000");
            assertThat(r.getCreadoPor()).isEqualTo("Administrador");
            assertThat(r.getPendientesParaPublicar()).contains("asigna un docente titular");
            verify(cursoDocenteRepository, never()).save(any());
        }

        @Test
        @DisplayName("Curso con docente titular activo → registra la asignación como titular")
        void crearConDocente() {
            CrearCursoRequest req = valido();
            req.setIdDocenteTitular(20);
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(20)).thenReturn(Optional.of(docente(20, 5, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 5)).thenReturn(Optional.empty());
            guardarDevuelveLoMismo();

            CursoResponse r = cursoService.crear(req, null);

            ArgumentCaptor<CursoDocente> captor = ArgumentCaptor.forClass(CursoDocente.class);
            verify(cursoDocenteRepository).save(captor.capture());
            assertThat(captor.getValue().getRol()).isEqualTo("titular");
            assertThat(r.getIdDocenteTitular()).isEqualTo(20);
            assertThat(r.getDocenteTitular()).isEqualTo("Julián Prada");
        }

        @Test
        @DisplayName("Borrador sin descripción, objetivos ni créditos → guarda \"\" y 0, no NULL (columnas NOT NULL en la base)")
        void borradorSinDatosOpcionales() {
            CrearCursoRequest req = CrearCursoRequest.builder().nombre("Curso mínimo").idArea(1).idGrado(1).build();
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado("activo")));
            guardarDevuelveLoMismo();

            CursoResponse r = cursoService.crear(req, null);

            ArgumentCaptor<Curso> captor = ArgumentCaptor.forClass(Curso.class);
            verify(cursoRepository).save(captor.capture());
            assertThat(captor.getValue().getDescripcion()).isEqualTo("");
            assertThat(captor.getValue().getObjetivos()).isEqualTo("");
            assertThat(captor.getValue().getCreditos()).isZero();
            // La respuesta muestra "sin dato" y el pendiente para publicar sigue apareciendo
            assertThat(r.getDescripcion()).isNull();
            assertThat(r.getCreditos()).isNull();
            assertThat(r.getPendientesParaPublicar()).contains("agrega una descripción");
        }

        @Test
        @DisplayName("Sin precio → se guarda en 0")
        void precioPorDefecto() {
            CrearCursoRequest req = valido();
            req.setPrecio(null);
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado("activo")));
            guardarDevuelveLoMismo();

            assertThat(cursoService.crear(req, null).getPrecio()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("Nombre repetido entre cursos no archivados → 409")
        void nombreRepetido() {
            when(cursoRepository.existsByNombreIgnoreCaseAndEstadoNot("Genética básica", "archivado")).thenReturn(true);

            assertThatThrownBy(() -> cursoService.crear(valido(), 7))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Ya existe un curso con ese nombre");
            verify(cursoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Área inexistente → 404")
        void areaInexistente() {
            when(areaRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.crear(valido(), 7))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El área académica indicada no existe");
        }

        @Test
        @DisplayName("Grado inexistente → 404")
        void gradoInexistente() {
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.crear(valido(), 7))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El grado indicado no existe");
        }

        @Test
        @DisplayName("Docente titular inactivo → 409 y no se crea el curso")
        void docenteInactivo() {
            CrearCursoRequest req = valido();
            req.setIdDocenteTitular(20);
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(20)).thenReturn(Optional.of(docente(20, 5, EstadoUsuario.inactivo)));

            assertThatThrownBy(() -> cursoService.crear(req, 7))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("El docente no está activo");
            verify(cursoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Docente inexistente → 404")
        void docenteInexistente() {
            CrearCursoRequest req = valido();
            req.setIdDocenteTitular(99);
            when(areaRepository.findById(1)).thenReturn(Optional.of(area("activo")));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.crear(req, 7))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El docente indicado no existe");
        }
    }

    // ── editar ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Editar cursos")
    class Editar {

        @Test
        @DisplayName("Solo cambian los campos enviados")
        void edicionParcial() {
            Curso c = curso("borrador");
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));
            guardarDevuelveLoMismo();

            CursoResponse r = cursoService.actualizar(10, ActualizarCursoRequest.builder().precio(new BigDecimal("50000")).build());

            assertThat(r.getPrecio()).isEqualByComparingTo("50000");
            assertThat(r.getNombre()).isEqualTo("Genética básica");
            assertThat(r.getDescripcion()).isEqualTo("Introducción a la herencia");
        }

        @Test
        @DisplayName("Renombrar con el nombre de otro curso → 409")
        void nombreDeOtro() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(curso("borrador")));
            when(cursoRepository.existsByNombreIgnoreCaseAndEstadoNotAndIdNot("Ecología", "archivado", 10)).thenReturn(true);

            assertThatThrownBy(() -> cursoService.actualizar(10, ActualizarCursoRequest.builder().nombre("Ecología").build()))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Ya existe otro curso con ese nombre");
        }

        @Test
        @DisplayName("Cambiar el titular → el anterior queda inactivo y el nuevo como titular")
        void cambiarTitular() {
            Curso c = cursoListo("activo");
            PerfilDocente nuevo = docente(30, 8, EstadoUsuario.activo);
            CursoDocente anterior = CursoDocente.builder().id(1).curso(c).docente(c.getDocenteTitular()).rol("titular").estado("activo").build();
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));
            when(perfilDocenteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(nuevo));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 5)).thenReturn(Optional.of(anterior));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8)).thenReturn(Optional.empty());
            guardarDevuelveLoMismo();

            CursoResponse r = cursoService.actualizar(10, ActualizarCursoRequest.builder().idDocenteTitular(30).build());

            assertThat(anterior.getEstado()).isEqualTo("inactivo");
            assertThat(r.getIdDocenteTitular()).isEqualTo(30);
            verify(cursoDocenteRepository, times(2)).save(any(CursoDocente.class));
        }

        @Test
        @DisplayName("Curso archivado → no se edita (409)")
        void archivadoNoSeEdita() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(curso("archivado")));

            assertThatThrownBy(() -> cursoService.actualizar(10, ActualizarCursoRequest.builder().precio(BigDecimal.ONE).build()))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("archivado");
        }

        @Test
        @DisplayName("Curso inexistente → 404")
        void inexistente() {
            when(cursoRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.actualizar(99, new ActualizarCursoRequest()))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El curso indicado no existe");
        }
    }

    // ── estados ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Cambio de estado")
    class Estados {

        @Test
        @DisplayName("Publicar un curso completo → activo")
        void publicar() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("borrador")));
            guardarDevuelveLoMismo();

            assertThat(cursoService.cambiarEstado(10, estado("activo")).getEstado()).isEqualTo("activo");
        }

        @Test
        @DisplayName("Publicar sin docente ni descripción → 409 indicando qué falta")
        void publicarIncompleto() {
            Curso c = curso("borrador");
            c.setDescripcion(" ");
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("activo")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("asigna un docente titular").hasMessageContaining("agrega una descripción");
        }

        @Test
        @DisplayName("Publicar sin lecciones activas → 409 (Fase 2)")
        void publicarSinLecciones() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("borrador")));
            when(leccionRepository.countByCurso_IdAndEstado(10, "activa")).thenReturn(0L);

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("activo")))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("agrega al menos una lección activa");
        }

        @Test
        @DisplayName("Publicar con el área inactiva → 409")
        void publicarAreaInactiva() {
            Curso c = cursoListo("borrador");
            c.setArea(area("inactivo"));
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("activo")))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("el área académica está inactiva");
        }

        @Test
        @DisplayName("Publicar con el titular desactivado → 409")
        void publicarTitularInactivo() {
            Curso c = curso("borrador");
            c.setDocenteTitular(docente(20, 5, EstadoUsuario.suspendido));
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("activo")))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("el docente titular no está activo");
        }

        @Test
        @DisplayName("Cerrar un curso activo y reabrirlo")
        void cerrarYReabrir() {
            Curso c = cursoListo("activo");
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));
            guardarDevuelveLoMismo();

            assertThat(cursoService.cambiarEstado(10, estado("cerrado")).getEstado()).isEqualTo("cerrado");
            assertThat(cursoService.cambiarEstado(10, estado("activo")).getEstado()).isEqualTo("activo");
        }

        @Test
        @DisplayName("Archivar y restaurar a borrador")
        void archivarYRestaurar() {
            Curso c = cursoListo("cerrado");
            when(cursoRepository.findById(10)).thenReturn(Optional.of(c));
            guardarDevuelveLoMismo();

            assertThat(cursoService.cambiarEstado(10, estado("archivado")).getEstado()).isEqualTo("archivado");
            assertThat(cursoService.cambiarEstado(10, estado("borrador")).getEstado()).isEqualTo("borrador");
        }

        @Test
        @DisplayName("Transición no permitida (borrador → cerrado) → 409 con las permitidas")
        void transicionInvalida() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(curso("borrador")));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("cerrado")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("No se puede pasar un curso de borrador a cerrado").hasMessageContaining("activo, archivado");
        }

        @Test
        @DisplayName("Un archivado solo puede volver a borrador (no a activo) → 409")
        void archivadoAActivo() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("archivado")));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("activo")))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("solo se permite: borrador");
        }

        @Test
        @DisplayName("Mismo estado → 409")
        void mismoEstado() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(curso("borrador")));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("borrador")))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El curso ya está en estado borrador");
        }

        @Test
        @DisplayName("Estado inexistente → IllegalArgumentException (400)")
        void estadoInvalido() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(curso("borrador")));

            assertThatThrownBy(() -> cursoService.cambiarEstado(10, estado("eliminado")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("borrador, activo, cerrado o archivado");
        }
    }

    // ── docentes ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Docentes del curso")
    class Docentes {

        @Test
        @DisplayName("Agregar un colaborador activo → se guarda con su rol")
        void agregarColaborador() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(docente(30, 8, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8)).thenReturn(Optional.empty());

            cursoService.agregarDocente(10, new AgregarDocenteCursoRequest(30, "Colaborador"));

            ArgumentCaptor<CursoDocente> captor = ArgumentCaptor.forClass(CursoDocente.class);
            verify(cursoDocenteRepository).save(captor.capture());
            assertThat(captor.getValue().getRol()).isEqualTo("colaborador");
            assertThat(captor.getValue().getEstado()).isEqualTo("activo");
        }

        @Test
        @DisplayName("Docente ya asignado y activo → 409")
        void duplicado() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(docente(30, 8, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8))
                    .thenReturn(Optional.of(CursoDocente.builder().rol("invitado").estado("activo").build()));

            assertThatThrownBy(() -> cursoService.agregarDocente(10, new AgregarDocenteCursoRequest(30, "colaborador")))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Ese docente ya está asignado a este curso");
        }

        @Test
        @DisplayName("Docente retirado antes → se reactiva con el nuevo rol")
        void reactivar() {
            CursoDocente retirado = CursoDocente.builder().id(4).rol("colaborador").estado("inactivo").build();
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(docente(30, 8, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8)).thenReturn(Optional.of(retirado));

            cursoService.agregarDocente(10, new AgregarDocenteCursoRequest(30, "invitado"));

            assertThat(retirado.getEstado()).isEqualTo("activo");
            assertThat(retirado.getRol()).isEqualTo("invitado");
        }

        @Test
        @DisplayName("Rol titular o desconocido → 400")
        void rolInvalido() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));

            assertThatThrownBy(() -> cursoService.agregarDocente(10, new AgregarDocenteCursoRequest(30, "titular")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("editando el curso");
            assertThatThrownBy(() -> cursoService.agregarDocente(10, new AgregarDocenteCursoRequest(30, "monitor")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("colaborador o invitado");
        }

        @Test
        @DisplayName("Retirar un colaborador → queda inactivo (se conserva el historial)")
        void retirar() {
            CursoDocente colaborador = CursoDocente.builder().id(4).rol("colaborador").estado("activo").build();
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(docente(30, 8, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8)).thenReturn(Optional.of(colaborador));

            cursoService.retirarDocente(10, 30);

            assertThat(colaborador.getEstado()).isEqualTo("inactivo");
            verify(cursoDocenteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Retirar al titular → 409")
        void retirarTitular() {
            CursoDocente titular = CursoDocente.builder().id(1).rol("titular").estado("activo").build();
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(20)).thenReturn(Optional.of(docente(20, 5, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 5)).thenReturn(Optional.of(titular));

            assertThatThrownBy(() -> cursoService.retirarDocente(10, 20))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("No puedes retirar al docente titular");
        }

        @Test
        @DisplayName("Retirar a un docente que no está en el curso → 404")
        void retirarNoAsignado() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(perfilDocenteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(docente(30, 8, EstadoUsuario.activo)));
            when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.retirarDocente(10, 30))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El docente no está asignado a este curso");
        }
    }

    // ── horarios ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Horarios")
    class Horarios {

        private HorarioRequest h(String dia, String ini, String fin) {
            return new HorarioRequest(dia, LocalTime.parse(ini), LocalTime.parse(fin));
        }

        @Test
        @DisplayName("Horarios válidos → reemplazan los anteriores; el día se normaliza sin tildes")
        void validos() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));

            cursoService.definirHorarios(10, new HorariosCursoRequest(List.of(h("Miércoles", "08:00", "10:00"), h("lunes", "14:00", "16:00"))));

            verify(horarioRepository).deleteByCurso_Id(10);
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<HorarioCurso>> captor = ArgumentCaptor.forClass(List.class);
            verify(horarioRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(HorarioCurso::getDiaSemana).containsExactly("miercoles", "lunes");
        }

        @Test
        @DisplayName("Fin antes del inicio → 400 y no se borra nada")
        void finAntesDeInicio() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));

            assertThatThrownBy(() -> cursoService.definirHorarios(10, new HorariosCursoRequest(List.of(h("lunes", "10:00", "08:00")))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("posterior a la de inicio");
            verify(horarioRepository, never()).deleteByCurso_Id(any());
        }

        @Test
        @DisplayName("Horarios que se cruzan el mismo día → 400")
        void cruce() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));

            assertThatThrownBy(() -> cursoService.definirHorarios(10,
                    new HorariosCursoRequest(List.of(h("martes", "08:00", "10:00"), h("martes", "09:30", "11:00")))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Los horarios del martes se cruzan");
        }

        @Test
        @DisplayName("Día inexistente → 400")
        void diaInvalido() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));

            assertThatThrownBy(() -> cursoService.definirHorarios(10, new HorariosCursoRequest(List.of(h("feriado", "08:00", "10:00")))))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Día no válido");
        }
    }

    // ── instituciones ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Instituciones que contratan el curso")
    class Instituciones {

        private Institucion inst(String estado) {
            return Institucion.builder().id(4).nombre("Colegio San Rafael").estado(estado).build();
        }

        @Test
        @DisplayName("Contratar con una institución activa → se registra")
        void contratar() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst("activa")));
            when(contratacionRepository.existsByCurso_IdAndInstitucion_Id(10, 4)).thenReturn(false);

            cursoService.contratar(10, new ContratarCursoRequest(4));

            verify(contratacionRepository).save(any(InstitucionCursoContratado.class));
        }

        @Test
        @DisplayName("Fase 3: contratar genera un código de inscripción de 8 caracteres")
        void contratarGeneraCodigo() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst("activa")));
            cursoService.contratar(10, new ContratarCursoRequest(4));
            verify(contratacionRepository).save(argThat(c -> c.getCodigoInscripcion() != null && c.getCodigoInscripcion().matches("[A-HJ-NP-Z2-9]{8}")));
        }

        @Test
        @DisplayName("Fase 3: no se quita una contratación con estudiantes inscritos con su código → 409")
        void quitarConInscritos() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            InstitucionCursoContratado ct = InstitucionCursoContratado.builder().id(7).build();
            when(contratacionRepository.findByCurso_IdAndInstitucion_Id(10, 4)).thenReturn(Optional.of(ct));
            when(inscripcionRepository.existsByContratacion_IdAndEstadoIn(eq(7), any())).thenReturn(true);
            assertThatThrownBy(() -> cursoService.quitarContratacion(10, 4))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("hay estudiantes inscritos");
            verify(contratacionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Fase 3: regenerar el código de una contratación")
        void regenerarCodigo() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            InstitucionCursoContratado ct = InstitucionCursoContratado.builder().id(7).codigoInscripcion("AAAAAAAA").build();
            when(contratacionRepository.findByCurso_IdAndInstitucion_Id(10, 4)).thenReturn(Optional.of(ct));
            cursoService.regenerarCodigo(10, 4);
            assertThat(ct.getCodigoInscripcion()).hasSize(8).isNotEqualTo("AAAAAAAA");
        }

        @Test
        @DisplayName("Contratación duplicada → 409")
        void duplicada() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst("activa")));
            when(contratacionRepository.existsByCurso_IdAndInstitucion_Id(10, 4)).thenReturn(true);

            assertThatThrownBy(() -> cursoService.contratar(10, new ContratarCursoRequest(4)))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("La institución ya tiene contratado este curso");
        }

        @Test
        @DisplayName("Institución inactiva → 409")
        void institucionInactiva() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst("suspendida")));

            assertThatThrownBy(() -> cursoService.contratar(10, new ContratarCursoRequest(4)))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no está activa");
        }

        @Test
        @DisplayName("Curso archivado → no se contrata (409)")
        void cursoArchivado() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("archivado")));

            assertThatThrownBy(() -> cursoService.contratar(10, new ContratarCursoRequest(4)))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Un curso archivado no se puede contratar");
        }

        @Test
        @DisplayName("Quitar una contratación existente → se elimina; si no existe → 404")
        void quitar() {
            InstitucionCursoContratado x = InstitucionCursoContratado.builder().id(2).build();
            when(cursoRepository.findById(10)).thenReturn(Optional.of(cursoListo("activo")));
            when(contratacionRepository.findByCurso_IdAndInstitucion_Id(10, 4)).thenReturn(Optional.of(x));
            when(contratacionRepository.findByCurso_IdAndInstitucion_Id(10, 99)).thenReturn(Optional.empty());

            cursoService.quitarContratacion(10, 4);
            verify(contratacionRepository).delete(x);
            assertThatThrownBy(() -> cursoService.quitarContratacion(10, 99))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("La institución no tiene contratado este curso");
        }
    }

    // ── consulta y catálogo ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Consulta y catálogo público")
    class Consulta {

        @Test
        @DisplayName("Listado con búsqueda sin tildes y filtro por docente")
        void listarConFiltros() {
            Curso genetica = cursoListo("activo");
            Curso ecologia = curso("borrador");
            ecologia.setId(11);
            ecologia.setNombre("Ecología");
            when(cursoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(ecologia, genetica));

            assertThat(cursoService.listar(null, null, null, null, "GENETICA")).extracting(CursoResponse::getId).containsExactly(10);
            assertThat(cursoService.listar(null, null, null, 20, null)).extracting(CursoResponse::getId).containsExactly(10);
            assertThat(cursoService.listar(null, 1, 1, null, "julian")).extracting(CursoResponse::getId).containsExactly(10);
        }

        @Test
        @DisplayName("Filtro por estado inexistente → 400")
        void estadoInvalido() {
            assertThatThrownBy(() -> cursoService.listar("publicado", null, null, null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Catálogo público → solo consulta cursos activos")
        void catalogoSoloActivos() {
            when(cursoRepository.findByEstadoOrderByNombreAsc("activo")).thenReturn(List.of(cursoListo("activo")));

            List<CursoCatalogoResponse> r = cursoService.catalogo(null, null, null);

            assertThat(r).hasSize(1);
            assertThat(r.get(0).getDocenteTitular()).isEqualTo("Julián Prada");
            verify(cursoRepository, never()).findAllByOrderByNombreAsc();
        }

        @Test
        @DisplayName("Catálogo público: los cursos «solo para colegios» no se ofrecen (ni en el detalle)")
        void catalogoSinCursosInstitucionales() {
            Curso abierto = cursoListo("activo");
            Curso institucional = cursoListo("activo");
            institucional.setId(77);
            institucional.setSoloInstitucional(true);
            when(cursoRepository.findByEstadoOrderByNombreAsc("activo")).thenReturn(List.of(abierto, institucional));
            assertThat(cursoService.catalogo(null, null, null)).hasSize(1);
            when(cursoRepository.findById(77)).thenReturn(Optional.of(institucional));
            assertThatThrownBy(() -> cursoService.detalleCatalogo(77)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Detalle público de un curso en borrador → 404")
        void detallePublicoBorrador() {
            when(cursoRepository.findById(10)).thenReturn(Optional.of(curso("borrador")));

            assertThatThrownBy(() -> cursoService.detalleCatalogo(10))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("El curso no existe o no está disponible");
        }

        @Test
        @DisplayName("Detalle de administración de un curso inexistente → 404")
        void detalleInexistente() {
            when(cursoRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cursoService.obtener(99)).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }
}
