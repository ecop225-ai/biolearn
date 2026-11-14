package co.biolearn.service.impl;

import co.biolearn.dto.ActualizarInstitucionRequest;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.CrearInstitucionRequest;
import co.biolearn.dto.InstitucionResponse;
import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Institucion;
import co.biolearn.model.SupervisorInstitucion;
import co.biolearn.model.Usuario;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.repository.SupervisorInstitucionRepository;
import co.biolearn.service.GeneradorCodigoAfiliacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InstitucionServiceImpl — módulo de Instituciones")
class InstitucionServiceImplTest {

    @Mock private InstitucionRepository institucionRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private SupervisorInstitucionRepository supervisorRepository;
    @Mock private GeneradorCodigoAfiliacion generadorCodigo;

    @InjectMocks private InstitucionServiceImpl institucionService;

    @BeforeEach
    void configurarComunes() {
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(inv -> {
            Institucion i = inv.getArgument(0);
            if (i.getId() == null) i.setId(10);
            return i;
        });
        when(generadorCodigo.generarUnico()).thenReturn("K7M2QX9P");
    }

    private Institucion institucion(int id, String estado, Integer cupos) {
        return Institucion.builder().id(id).nombre("Colegio San Rafael").nit("900.223.114-2")
                .codigoAfiliacion("SR7K2MQX").estado(estado).cuposContratados(cupos).build();
    }

    private CrearInstitucionRequest solicitud() {
        CrearInstitucionRequest r = new CrearInstitucionRequest();
        r.setNombre("  Colegio Los Andes ");
        r.setNit("901.555.123-4");
        r.setCorreo("Rectoria@LosAndes.edu.co");
        r.setCuposContratados(80);
        return r;
    }

    private CambiarEstadoRequest estado(String valor) {
        CambiarEstadoRequest r = new CambiarEstadoRequest();
        r.setEstado(valor);
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Registro de instituciones")
    class Registro {

        @Test
        @DisplayName("Registro válido → activa, con código generado y datos normalizados")
        void registroValido() {
            InstitucionResponse r = institucionService.crear(solicitud());

            ArgumentCaptor<Institucion> captor = ArgumentCaptor.forClass(Institucion.class);
            verify(institucionRepository).save(captor.capture());
            Institucion guardada = captor.getValue();
            assertThat(guardada.getCodigoAfiliacion()).isEqualTo("K7M2QX9P");
            assertThat(guardada.getEstado()).isEqualTo("activa");
            assertThat(guardada.getNombre()).isEqualTo("Colegio Los Andes");
            assertThat(guardada.getCorreo()).isEqualTo("rectoria@losandes.edu.co");
            assertThat(r.getCuposContratados()).isEqualTo(80);
            assertThat(r.getCuposDisponibles()).isEqualTo(80);
        }

        @Test
        @DisplayName("NIT repetido → 409 y no se guarda nada")
        void nitRepetido() {
            when(institucionRepository.existsByNit("901.555.123-4")).thenReturn(true);

            assertThatThrownBy(() -> institucionService.crear(solicitud()))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ya existe una institución registrada con ese NIT");
            verify(institucionRepository, never()).save(any(Institucion.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Consulta y listado")
    class Consulta {

        @Test
        @DisplayName("Detalle → incluye cupos usados, disponibles y representante activo")
        void detalleCompleto() {
            when(institucionRepository.findById(4)).thenReturn(Optional.of(institucion(4, "activa", 120)));
            when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(30L);
            Usuario marta = Usuario.builder().id(8).nombre("Marta").apellido("Loaiza").correo("marta@test.com")
                    .tipoUsuario(RolUsuario.supervisor).estado(EstadoUsuario.pendiente).build();
            when(supervisorRepository.findFirstByInstitucion_IdAndEstado(4, EstadoPerfilAmplio.activo))
                    .thenReturn(Optional.of(SupervisorInstitucion.builder().usuario(marta).cargo("Rectora").build()));

            InstitucionResponse r = institucionService.obtener(4);

            assertThat(r.getCuposUsados()).isEqualTo(30);
            assertThat(r.getCuposDisponibles()).isEqualTo(90);
            assertThat(r.getRepresentante().getNombreCompleto()).isEqualTo("Marta Loaiza");
            assertThat(r.getRepresentante().getEstadoCuenta()).isEqualTo("pendiente");
        }

        @Test
        @DisplayName("Institución sin límite de cupos → cuposDisponibles es null")
        void sinLimiteDeCupos() {
            when(institucionRepository.findById(1)).thenReturn(Optional.of(institucion(1, "activa", null)));

            assertThat(institucionService.obtener(1).getCuposDisponibles()).isNull();
        }

        @Test
        @DisplayName("Institución inexistente → 404")
        void inexistente() {
            when(institucionRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> institucionService.obtener(999))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("La institución indicada no existe");
        }

        @Test
        @DisplayName("Búsqueda sin tildes ni mayúsculas por nombre, NIT o código")
        void busqueda() {
            Institucion andina = Institucion.builder().id(2).nombre("Corporación Andina").nit("800.556.902-1")
                    .codigoAfiliacion("AN4P8TWZ").estado("activa").build();
            when(institucionRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(institucion(4, "activa", 120), andina));

            assertThat(institucionService.listar(null, "corporacion")).extracting(InstitucionResponse::getId).containsExactly(2);
            assertThat(institucionService.listar(null, "sr7k")).extracting(InstitucionResponse::getId).containsExactly(4);
        }

        @Test
        @DisplayName("Filtrar por estado → consulta solo ese estado")
        void filtrarPorEstado() {
            institucionService.listar("Inactiva", null);

            verify(institucionRepository).findByEstadoOrderByNombreAsc("inactiva");
        }

        @Test
        @DisplayName("Filtrar por estado inexistente → IllegalArgumentException (400)")
        void estadoInexistente() {
            assertThatThrownBy(() -> institucionService.listar("cerrada", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Edición, estado y código")
    class Edicion {

        @Test
        @DisplayName("Editar datos y cupos por encima de los afiliados → se actualiza")
        void editarValido() {
            Institucion inst = institucion(4, "activa", 120);
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst));
            when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(30L);
            ActualizarInstitucionRequest r = new ActualizarInstitucionRequest();
            r.setRector(" Pedro Gómez ");
            r.setCuposContratados(30);

            institucionService.actualizar(4, r);

            assertThat(inst.getRector()).isEqualTo("Pedro Gómez");
            assertThat(inst.getCuposContratados()).isEqualTo(30);
        }

        @Test
        @DisplayName("Cupos por debajo de los estudiantes afiliados → 409")
        void cuposInsuficientes() {
            Institucion inst = institucion(4, "activa", 120);
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst));
            when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(30L);
            ActualizarInstitucionRequest r = new ActualizarInstitucionRequest();
            r.setCuposContratados(29);

            assertThatThrownBy(() -> institucionService.actualizar(4, r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("ya tiene 30 estudiantes activos afiliados");
            assertThat(inst.getCuposContratados()).isEqualTo(120);
        }

        @Test
        @DisplayName("NIT de otra institución → 409")
        void nitDeOtra() {
            when(institucionRepository.findById(4)).thenReturn(Optional.of(institucion(4, "activa", 120)));
            when(institucionRepository.existsByNitAndIdNot("800.556.902-1", 4)).thenReturn(true);
            ActualizarInstitucionRequest r = new ActualizarInstitucionRequest();
            r.setNit("800.556.902-1");

            assertThatThrownBy(() -> institucionService.actualizar(4, r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ya existe otra institución registrada con ese NIT");
        }

        @Test
        @DisplayName("Correo en blanco → se elimina el correo")
        void correoEnBlanco() {
            Institucion inst = institucion(4, "activa", 120);
            inst.setCorreo("viejo@test.com");
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst));
            ActualizarInstitucionRequest r = new ActualizarInstitucionRequest();
            r.setCorreo("  ");

            institucionService.actualizar(4, r);

            assertThat(inst.getCorreo()).isNull();
        }

        @Test
        @DisplayName("Cambiar estado a suspendida → se actualiza en minúsculas")
        void cambiarEstado() {
            Institucion inst = institucion(4, "activa", 120);
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst));

            assertThat(institucionService.cambiarEstado(4, estado("Suspendida")).getEstado()).isEqualTo("suspendida");
        }

        @Test
        @DisplayName("Cambiar a un estado inexistente → IllegalArgumentException (400)")
        void cambiarEstadoInvalido() {
            when(institucionRepository.findById(4)).thenReturn(Optional.of(institucion(4, "activa", 120)));

            assertThatThrownBy(() -> institucionService.cambiarEstado(4, estado("pendiente")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activa, inactiva o suspendida");
        }

        @Test
        @DisplayName("Regenerar código → el nuevo es distinto del anterior")
        void regenerarCodigo() {
            Institucion inst = institucion(4, "activa", 120); // código actual SR7K2MQX
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst));
            when(generadorCodigo.generarUnico()).thenReturn("SR7K2MQX", "N3W8CDDE");

            InstitucionResponse r = institucionService.regenerarCodigo(4);

            assertThat(r.getCodigoAfiliacion()).isNotEqualTo("SR7K2MQX").isEqualTo("N3W8CDDE");
        }
    }
}
