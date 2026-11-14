package co.biolearn.service.impl;

import co.biolearn.dto.ActualizarEstadoUsuarioRequest;
import co.biolearn.dto.ActualizarUsuarioRequest;
import co.biolearn.dto.CrearUsuarioRequest;
import co.biolearn.dto.UsuarioAdminResponse;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolContador;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Institucion;
import co.biolearn.model.PerfilContador;
import co.biolearn.model.PerfilDocente;
import co.biolearn.model.SupervisorInstitucion;
import co.biolearn.model.Usuario;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.PerfilContadorRepository;
import co.biolearn.repository.PerfilDocenteRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.repository.SupervisorInstitucionRepository;
import co.biolearn.repository.UsuarioRepository;
import co.biolearn.service.TokenVerificacionService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UsuarioServiceImpl — módulo de Gestión de Usuarios")
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PerfilDocenteRepository perfilDocenteRepository;
    @Mock private PerfilContadorRepository perfilContadorRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private SupervisorInstitucionRepository supervisorRepository;
    @Mock private InstitucionRepository institucionRepository;
    @Mock private TokenVerificacionService tokenService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UsuarioServiceImpl usuarioService;

    private static final int ID_ADMIN = 1;

    @BeforeEach
    void configurarComunes() {
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
    }

    private Usuario usuario(int id, RolUsuario rol, EstadoUsuario estado) {
        return Usuario.builder().id(id).nombre("Nombre").apellido("Apellido").nombreUsuario("user" + id)
                .correo("user" + id + "@test.com").tipoUsuario(rol).estado(estado).build();
    }

    private Institucion institucion(int id, String estado) {
        return Institucion.builder().id(id).nombre("Institución " + id).codigoAfiliacion("SR7K2MQX").estado(estado).build();
    }

    private CrearUsuarioRequest solicitud(String rol) {
        CrearUsuarioRequest r = new CrearUsuarioRequest();
        r.setNombre("Marta");
        r.setApellido("Loaiza");
        r.setNombreUsuario("marta.loaiza");
        r.setCorreo("Marta.Loaiza@Test.com");
        r.setRol(rol);
        return r;
    }

    private ActualizarEstadoUsuarioRequest estado(String valor) {
        ActualizarEstadoUsuarioRequest r = new ActualizarEstadoUsuarioRequest();
        r.setEstado(valor);
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Creación de usuarios")
    class Creacion {

        @Test
        @DisplayName("Docente → cuenta pendiente con invitación de 7 días")
        void crearDocente() {
            CrearUsuarioRequest r = solicitud("docente");
            r.setEspecialidad("Genética");

            UsuarioAdminResponse creado = usuarioService.crearUsuario(r);

            assertThat(creado.getEstado()).isEqualTo(EstadoUsuario.pendiente);
            assertThat(creado.getCorreo()).isEqualTo("marta.loaiza@test.com");
            verify(perfilDocenteRepository).save(any(PerfilDocente.class));
            verify(tokenService).crearTokenActivacion(any(Usuario.class),
                    eq(TokenVerificacionService.HORAS_INVITACION_PERSONAL), anyString());
        }

        @Test
        @DisplayName("Rol estudiante → rechazado (los estudiantes se autorregistran)")
        void rolEstudianteRechazado() {
            assertThatThrownBy(() -> usuarioService.crearUsuario(solicitud("estudiante")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("Los estudiantes se registran por su cuenta");
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Correo repetido → 409")
        void correoRepetido() {
            when(usuarioRepository.existsByCorreo("marta.loaiza@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(solicitud("docente")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ya existe una cuenta registrada con ese correo");
        }

        @Test
        @DisplayName("Contador con rolContador inválido → IllegalArgumentException (400)")
        void rolContadorInvalido() {
            CrearUsuarioRequest r = solicitud("contador");
            r.setRolContador("jefe");

            assertThatThrownBy(() -> usuarioService.crearUsuario(r))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rolContador no válido");
        }

        @Test
        @DisplayName("Supervisor sin institución → 409")
        void supervisorSinInstitucion() {
            assertThatThrownBy(() -> usuarioService.crearUsuario(solicitud("supervisor")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Debes indicar la institución que representará el supervisor");
        }

        @Test
        @DisplayName("Supervisor para institución inexistente → 404")
        void institucionInexistente() {
            CrearUsuarioRequest r = solicitud("supervisor");
            r.setIdInstitucion(999);
            when(institucionRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.crearUsuario(r))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("La institución indicada no existe");
        }

        @Test
        @DisplayName("Supervisor para institución inactiva → 409")
        void institucionInactiva() {
            CrearUsuarioRequest r = solicitud("supervisor");
            r.setIdInstitucion(5);
            when(institucionRepository.findById(5)).thenReturn(Optional.of(institucion(5, "inactiva")));

            assertThatThrownBy(() -> usuarioService.crearUsuario(r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("no está activa");
        }

        @Test
        @DisplayName("Institución que ya tiene representante activo → 409 (regla de representante único)")
        void representanteDuplicado() {
            CrearUsuarioRequest r = solicitud("supervisor");
            r.setIdInstitucion(2);
            when(institucionRepository.findById(2)).thenReturn(Optional.of(institucion(2, "activa")));
            when(supervisorRepository.existsByInstitucion_IdAndEstado(2, EstadoPerfilAmplio.activo)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Esta institución ya tiene un representante activo asignado");
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Institución libre → supervisor creado y vinculado")
        void supervisorCreado() {
            CrearUsuarioRequest r = solicitud("supervisor");
            r.setIdInstitucion(2);
            r.setCargo("Rectora");
            when(institucionRepository.findById(2)).thenReturn(Optional.of(institucion(2, "activa")));

            usuarioService.crearUsuario(r);

            verify(supervisorRepository).save(any(SupervisorInstitucion.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Cambio de estado")
    class CambioEstado {

        @Test
        @DisplayName("El administrador no puede cambiar su propio estado")
        void propiaCuenta() {
            when(usuarioRepository.findById(ID_ADMIN)).thenReturn(Optional.of(usuario(ID_ADMIN, RolUsuario.admin, EstadoUsuario.activo)));

            assertThatThrownBy(() -> usuarioService.actualizarEstado(ID_ADMIN, estado("inactivo"), ID_ADMIN))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("No puedes cambiar el estado de tu propia cuenta");
        }

        @Test
        @DisplayName("El estado 'pendiente' no se puede asignar manualmente")
        void estadoPendiente() {
            when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario(7, RolUsuario.docente, EstadoUsuario.activo)));

            assertThatThrownBy(() -> usuarioService.actualizarEstado(7, estado("pendiente"), ID_ADMIN))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("Estado no válido");
        }

        @Test
        @DisplayName("Desactivar un supervisor libera la institución (vínculo pasa a inactivo)")
        void desactivarSupervisor() {
            Usuario diego = usuario(8, RolUsuario.supervisor, EstadoUsuario.activo);
            SupervisorInstitucion vinculo = SupervisorInstitucion.builder().usuario(diego)
                    .institucion(institucion(2, "activa")).estado(EstadoPerfilAmplio.activo).build();
            when(usuarioRepository.findById(8)).thenReturn(Optional.of(diego));
            when(supervisorRepository.findByUsuario_Id(8)).thenReturn(Optional.of(vinculo));

            usuarioService.actualizarEstado(8, estado("inactivo"), ID_ADMIN);

            assertThat(diego.getEstado()).isEqualTo(EstadoUsuario.inactivo);
            assertThat(vinculo.getEstado()).isEqualTo(EstadoPerfilAmplio.inactivo);
            verify(supervisorRepository).save(vinculo);
        }

        @Test
        @DisplayName("Reactivar un supervisor cuando otro ya representa la institución → 409")
        void reactivarConOtroActivo() {
            Usuario diego = usuario(8, RolUsuario.supervisor, EstadoUsuario.inactivo);
            SupervisorInstitucion vinculo = SupervisorInstitucion.builder().usuario(diego)
                    .institucion(institucion(2, "activa")).estado(EstadoPerfilAmplio.inactivo).build();
            when(usuarioRepository.findById(8)).thenReturn(Optional.of(diego));
            when(supervisorRepository.findByUsuario_Id(8)).thenReturn(Optional.of(vinculo));
            when(supervisorRepository.existsByInstitucion_IdAndEstadoAndUsuario_IdNot(2, EstadoPerfilAmplio.activo, 8))
                    .thenReturn(true);

            assertThatThrownBy(() -> usuarioService.actualizarEstado(8, estado("activo"), ID_ADMIN))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("No se puede reactivar: la institución ya tiene otro representante activo");
            assertThat(diego.getEstado()).isEqualTo(EstadoUsuario.inactivo);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Invitaciones, edición y listado")
    class Otros {

        @Test
        @DisplayName("Reenviar invitación a docente pendiente → genera token nuevo")
        void reenviarAPendiente() {
            when(usuarioRepository.findById(12)).thenReturn(Optional.of(usuario(12, RolUsuario.docente, EstadoUsuario.pendiente)));

            usuarioService.reenviarInvitacion(12);

            verify(tokenService).crearTokenActivacion(any(Usuario.class), anyInt(), anyString());
        }

        @Test
        @DisplayName("Reenviar invitación a cuenta ya activa → 409")
        void reenviarACuentaActiva() {
            when(usuarioRepository.findById(12)).thenReturn(Optional.of(usuario(12, RolUsuario.docente, EstadoUsuario.activo)));

            assertThatThrownBy(() -> usuarioService.reenviarInvitacion(12))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("ya fue activada");
        }

        @Test
        @DisplayName("Reenviar invitación a un estudiante → 409")
        void reenviarAEstudiante() {
            when(usuarioRepository.findById(3)).thenReturn(Optional.of(usuario(3, RolUsuario.estudiante, EstadoUsuario.pendiente)));

            assertThatThrownBy(() -> usuarioService.reenviarInvitacion(3))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("Solo se reenvían invitaciones");
        }

        @Test
        @DisplayName("Editar contador con rolContador inválido → IllegalArgumentException (400), igual que al crear")
        void editarRolContadorInvalido() {
            when(usuarioRepository.findById(6)).thenReturn(Optional.of(usuario(6, RolUsuario.contador, EstadoUsuario.activo)));
            when(perfilContadorRepository.findByUsuario_Id(6)).thenReturn(Optional.of(new PerfilContador()));
            ActualizarUsuarioRequest r = new ActualizarUsuarioRequest();
            r.setRolContador("jefe");

            assertThatThrownBy(() -> usuarioService.actualizarUsuario(6, r))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rolContador no válido");
        }

        @Test
        @DisplayName("Filtrar por institución inexistente → 404")
        void filtrarInstitucionInexistente() {
            when(institucionRepository.existsById(999)).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.listarUsuarios(null, null, 999))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("La institución indicada no existe");
        }

        @Test
        @DisplayName("Filtrar por institución → solo devuelve a quienes pertenecen a ella")
        void filtrarPorInstitucion() {
            Usuario deLaInst2 = usuario(8, RolUsuario.supervisor, EstadoUsuario.activo);
            Usuario deLaInst4 = usuario(9, RolUsuario.supervisor, EstadoUsuario.activo);
            Usuario docente = usuario(12, RolUsuario.docente, EstadoUsuario.activo);
            when(usuarioRepository.findAll()).thenReturn(List.of(deLaInst2, deLaInst4, docente));
            when(institucionRepository.existsById(2)).thenReturn(true);
            when(supervisorRepository.findByUsuario_Id(8)).thenReturn(Optional.of(SupervisorInstitucion.builder()
                    .usuario(deLaInst2).institucion(institucion(2, "activa")).build()));
            when(supervisorRepository.findByUsuario_Id(9)).thenReturn(Optional.of(SupervisorInstitucion.builder()
                    .usuario(deLaInst4).institucion(institucion(4, "activa")).build()));

            List<UsuarioAdminResponse> resultado = usuarioService.listarUsuarios(null, null, 2);

            assertThat(resultado).extracting(UsuarioAdminResponse::getId).containsExactly(8);
            assertThat(resultado.get(0).getInstitucion()).isEqualTo("Institución 2");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Edición de perfil y consulta de detalle")
    class EdicionYConsulta {

        @Test
        @DisplayName("Editar docente → actualiza campos comunes (sin espacios sobrantes) y especialidad")
        void editarDocente() {
            Usuario julian = usuario(6, RolUsuario.docente, EstadoUsuario.activo);
            PerfilDocente perfil = PerfilDocente.builder().usuario(julian).especialidad("Biología").build();
            when(usuarioRepository.findById(6)).thenReturn(Optional.of(julian));
            when(perfilDocenteRepository.findByUsuario_Id(6)).thenReturn(Optional.of(perfil));
            ActualizarUsuarioRequest r = new ActualizarUsuarioRequest();
            r.setNombre("  Julián Andrés ");
            r.setTelefono("3001234567");
            r.setCiudad(" Bogotá ");
            r.setEspecialidad("Biología molecular avanzada");

            UsuarioAdminResponse respuesta = usuarioService.actualizarUsuario(6, r);

            assertThat(julian.getNombre()).isEqualTo("Julián Andrés");
            assertThat(julian.getCiudad()).isEqualTo("Bogotá");
            assertThat(julian.getTelefono()).isEqualTo("3001234567");
            assertThat(perfil.getEspecialidad()).isEqualTo("Biología molecular avanzada");
            assertThat(respuesta.getEspecialidad()).isEqualTo("Biología molecular avanzada");
        }

        @Test
        @DisplayName("Nombre en blanco o campos ausentes → no se modifican")
        void camposEnBlancoNoCambian() {
            Usuario julian = usuario(6, RolUsuario.docente, EstadoUsuario.activo);
            when(usuarioRepository.findById(6)).thenReturn(Optional.of(julian));
            ActualizarUsuarioRequest r = new ActualizarUsuarioRequest();
            r.setNombre("   ");

            usuarioService.actualizarUsuario(6, r);

            assertThat(julian.getNombre()).isEqualTo("Nombre");
            verify(perfilDocenteRepository, never()).save(any(PerfilDocente.class));
        }

        @Test
        @DisplayName("Editar supervisor → actualiza el cargo")
        void editarSupervisor() {
            Usuario diego = usuario(8, RolUsuario.supervisor, EstadoUsuario.activo);
            SupervisorInstitucion vinculo = SupervisorInstitucion.builder().usuario(diego)
                    .institucion(institucion(2, "activa")).cargo("Coordinador").build();
            when(usuarioRepository.findById(8)).thenReturn(Optional.of(diego));
            when(supervisorRepository.findByUsuario_Id(8)).thenReturn(Optional.of(vinculo));
            ActualizarUsuarioRequest r = new ActualizarUsuarioRequest();
            r.setCargo("Rector");

            UsuarioAdminResponse respuesta = usuarioService.actualizarUsuario(8, r);

            assertThat(vinculo.getCargo()).isEqualTo("Rector");
            assertThat(respuesta.getCargo()).isEqualTo("Rector");
        }

        @Test
        @DisplayName("Editar contador con rolContador válido → se actualiza")
        void editarContadorValido() {
            PerfilContador perfil = new PerfilContador();
            when(usuarioRepository.findById(6)).thenReturn(Optional.of(usuario(6, RolUsuario.contador, EstadoUsuario.activo)));
            when(perfilContadorRepository.findByUsuario_Id(6)).thenReturn(Optional.of(perfil));
            ActualizarUsuarioRequest r = new ActualizarUsuarioRequest();
            r.setRolContador("Auditor");

            usuarioService.actualizarUsuario(6, r);

            assertThat(perfil.getRolContador()).isEqualTo(RolContador.auditor);
        }

        @Test
        @DisplayName("Editar usuario inexistente → 404")
        void editarInexistente() {
            when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.actualizarUsuario(999, new ActualizarUsuarioRequest()))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El usuario indicado no existe");
        }

        @Test
        @DisplayName("Editar docente cuyo perfil no existe → 404")
        void editarDocenteSinPerfil() {
            when(usuarioRepository.findById(6)).thenReturn(Optional.of(usuario(6, RolUsuario.docente, EstadoUsuario.activo)));
            when(perfilDocenteRepository.findByUsuario_Id(6)).thenReturn(Optional.empty());
            ActualizarUsuarioRequest r = new ActualizarUsuarioRequest();
            r.setEspecialidad("Genética");

            assertThatThrownBy(() -> usuarioService.actualizarUsuario(6, r))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Perfil de docente no encontrado");
        }

        @Test
        @DisplayName("Detalle de un contador → incluye su rol de contador")
        void detalleContador() {
            PerfilContador perfil = new PerfilContador();
            perfil.setRolContador(RolContador.administrador_financiero);
            when(usuarioRepository.findById(5)).thenReturn(Optional.of(usuario(5, RolUsuario.contador, EstadoUsuario.activo)));
            when(perfilContadorRepository.findByUsuario_Id(5)).thenReturn(Optional.of(perfil));

            assertThat(usuarioService.obtenerUsuario(5).getRolContador()).isEqualTo("administrador_financiero");
        }

        @Test
        @DisplayName("Detalle de usuario inexistente → 404")
        void detalleInexistente() {
            when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.obtenerUsuario(999))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El usuario indicado no existe");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Filtros del listado y casos adicionales")
    class FiltrosYAdicionales {

        @Test
        @DisplayName("Filtrar solo por rol → consulta por tipo de usuario")
        void filtrarPorRol() {
            when(usuarioRepository.findByTipoUsuario(RolUsuario.docente))
                    .thenReturn(List.of(usuario(6, RolUsuario.docente, EstadoUsuario.activo)));

            assertThat(usuarioService.listarUsuarios("Docente", null, null)).hasSize(1);
            verify(usuarioRepository).findByTipoUsuario(RolUsuario.docente);
        }

        @Test
        @DisplayName("Filtrar solo por estado → consulta por estado")
        void filtrarPorEstado() {
            usuarioService.listarUsuarios(null, "pendiente", null);

            verify(usuarioRepository).findByEstado(EstadoUsuario.pendiente);
        }

        @Test
        @DisplayName("Filtrar por rol y estado → consulta combinada")
        void filtrarPorRolYEstado() {
            usuarioService.listarUsuarios("estudiante", "activo", null);

            verify(usuarioRepository).findByTipoUsuarioAndEstado(RolUsuario.estudiante, EstadoUsuario.activo);
        }

        @Test
        @DisplayName("Rol inexistente en el filtro → IllegalArgumentException (400)")
        void filtrarRolInvalido() {
            assertThatThrownBy(() -> usuarioService.listarUsuarios("xyz", null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Crear con rol inexistente → 409 «Rol no reconocido»")
        void crearRolInexistente() {
            assertThatThrownBy(() -> usuarioService.crearUsuario(solicitud("xyz")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Rol no reconocido");
        }

        @Test
        @DisplayName("Crear con nombre de usuario repetido → 409")
        void crearNombreUsuarioRepetido() {
            when(usuarioRepository.existsByNombreUsuario("marta.loaiza")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.crearUsuario(solicitud("docente")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ese nombre de usuario ya está en uso, elige otro");
        }

        @Test
        @DisplayName("Crear contador sin rolContador → queda como auxiliar")
        void crearContadorPorDefecto() {
            usuarioService.crearUsuario(solicitud("contador"));

            org.mockito.ArgumentCaptor<PerfilContador> captor = org.mockito.ArgumentCaptor.forClass(PerfilContador.class);
            verify(perfilContadorRepository).save(captor.capture());
            assertThat(captor.getValue().getRolContador()).isEqualTo(RolContador.auxiliar);
        }

        @Test
        @DisplayName("Cambiar estado con valor inexistente → 409")
        void estadoInexistente() {
            when(usuarioRepository.findById(7)).thenReturn(Optional.of(usuario(7, RolUsuario.docente, EstadoUsuario.activo)));

            assertThatThrownBy(() -> usuarioService.actualizarEstado(7, estado("bloqueado"), ID_ADMIN))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("Estado no válido");
        }

        @Test
        @DisplayName("Cambiar estado de usuario inexistente → 404")
        void estadoUsuarioInexistente() {
            when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.actualizarEstado(999, estado("inactivo"), ID_ADMIN))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("Reactivar supervisor sin otro representante → el vínculo vuelve a activo")
        void reactivarSupervisorLibre() {
            Usuario diego = usuario(8, RolUsuario.supervisor, EstadoUsuario.inactivo);
            SupervisorInstitucion vinculo = SupervisorInstitucion.builder().usuario(diego)
                    .institucion(institucion(2, "activa")).estado(EstadoPerfilAmplio.inactivo).build();
            when(usuarioRepository.findById(8)).thenReturn(Optional.of(diego));
            when(supervisorRepository.findByUsuario_Id(8)).thenReturn(Optional.of(vinculo));

            usuarioService.actualizarEstado(8, estado("activo"), ID_ADMIN);

            assertThat(diego.getEstado()).isEqualTo(EstadoUsuario.activo);
            assertThat(vinculo.getEstado()).isEqualTo(EstadoPerfilAmplio.activo);
        }

        @Test
        @DisplayName("Suspender supervisor → el vínculo pasa a suspendido")
        void suspenderSupervisor() {
            Usuario diego = usuario(8, RolUsuario.supervisor, EstadoUsuario.activo);
            SupervisorInstitucion vinculo = SupervisorInstitucion.builder().usuario(diego)
                    .institucion(institucion(2, "activa")).estado(EstadoPerfilAmplio.activo).build();
            when(usuarioRepository.findById(8)).thenReturn(Optional.of(diego));
            when(supervisorRepository.findByUsuario_Id(8)).thenReturn(Optional.of(vinculo));

            usuarioService.actualizarEstado(8, estado("suspendido"), ID_ADMIN);

            assertThat(vinculo.getEstado()).isEqualTo(EstadoPerfilAmplio.suspendido);
        }

        @Test
        @DisplayName("Reenviar invitación a usuario inexistente → 404")
        void reenviarInexistente() {
            when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.reenviarInvitacion(999))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El usuario indicado no existe");
        }
    }

    @Nested
    @DisplayName("Asignación de institución a un representante existente")
    class AsignacionInstitucion {

        private Institucion inst(int id, String estado) {
            return Institucion.builder().id(id).nombre("Institución " + id).estado(estado).build();
        }

        private co.biolearn.dto.AsignarInstitucionRequest req(int idInstitucion, String cargo) {
            return new co.biolearn.dto.AsignarInstitucionRequest(idInstitucion, cargo);
        }

        @Test
        @DisplayName("Representante sin institución → se crea su registro activo con la institución y el cargo")
        void asignarSinInstitucion() {
            Usuario rep = usuario(15, RolUsuario.supervisor, EstadoUsuario.activo);
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(rep));
            when(institucionRepository.findById(8)).thenReturn(Optional.of(inst(8, "activa")));
            when(supervisorRepository.findByUsuario_Id(15)).thenReturn(Optional.empty());
            when(supervisorRepository.existsByInstitucion_IdAndEstadoAndUsuario_IdNot(8, EstadoPerfilAmplio.activo, 15)).thenReturn(false);

            usuarioService.asignarInstitucion(15, req(8, "Coordinadora académica"));

            ArgumentCaptor<SupervisorInstitucion> captor = ArgumentCaptor.forClass(SupervisorInstitucion.class);
            verify(supervisorRepository).save(captor.capture());
            assertThat(captor.getValue().getInstitucion().getId()).isEqualTo(8);
            assertThat(captor.getValue().getCargo()).isEqualTo("Coordinadora académica");
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPerfilAmplio.activo);
        }

        @Test
        @DisplayName("Cambiar de institución → actualiza el registro existente (no crea otro)")
        void cambiarInstitucion() {
            Usuario rep = usuario(15, RolUsuario.supervisor, EstadoUsuario.activo);
            SupervisorInstitucion perfil = SupervisorInstitucion.builder().id(3).usuario(rep).institucion(inst(4, "activa"))
                    .cargo("Rector").estado(EstadoPerfilAmplio.activo).build();
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(rep));
            when(institucionRepository.findById(8)).thenReturn(Optional.of(inst(8, "activa")));
            when(supervisorRepository.findByUsuario_Id(15)).thenReturn(Optional.of(perfil));

            usuarioService.asignarInstitucion(15, req(8, null));

            verify(supervisorRepository).save(perfil);
            assertThat(perfil.getId()).isEqualTo(3);
            assertThat(perfil.getInstitucion().getId()).isEqualTo(8);
            assertThat(perfil.getCargo()).isEqualTo("Rector");   // sin cargo nuevo, se conserva el anterior
        }

        @Test
        @DisplayName("Representante inactivo → la asignación queda inactiva y no ocupa el cupo de la institución")
        void representanteInactivo() {
            Usuario rep = usuario(15, RolUsuario.supervisor, EstadoUsuario.inactivo);
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(rep));
            when(institucionRepository.findById(8)).thenReturn(Optional.of(inst(8, "activa")));
            when(supervisorRepository.findByUsuario_Id(15)).thenReturn(Optional.empty());

            usuarioService.asignarInstitucion(15, req(8, null));

            ArgumentCaptor<SupervisorInstitucion> captor = ArgumentCaptor.forClass(SupervisorInstitucion.class);
            verify(supervisorRepository).save(captor.capture());
            assertThat(captor.getValue().getEstado()).isEqualTo(EstadoPerfilAmplio.inactivo);
        }

        @Test
        @DisplayName("Usuario que no es representante → 409")
        void noEsRepresentante() {
            when(usuarioRepository.findById(9)).thenReturn(Optional.of(usuario(9, RolUsuario.docente, EstadoUsuario.activo)));

            assertThatThrownBy(() -> usuarioService.asignarInstitucion(9, req(8, null)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Solo se puede asignar una institución a un representante institucional");
        }

        @Test
        @DisplayName("Institución inactiva → 409")
        void institucionInactiva() {
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(usuario(15, RolUsuario.supervisor, EstadoUsuario.activo)));
            when(institucionRepository.findById(2)).thenReturn(Optional.of(inst(2, "inactiva")));

            assertThatThrownBy(() -> usuarioService.asignarInstitucion(15, req(2, null)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("No se puede asignar un representante a una institución que no está activa");
        }

        @Test
        @DisplayName("Institución con otro representante activo → 409 (regla RF-004)")
        void otroRepresentante() {
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(usuario(15, RolUsuario.supervisor, EstadoUsuario.activo)));
            when(institucionRepository.findById(4)).thenReturn(Optional.of(inst(4, "activa")));
            when(supervisorRepository.findByUsuario_Id(15)).thenReturn(Optional.empty());
            when(supervisorRepository.existsByInstitucion_IdAndEstadoAndUsuario_IdNot(4, EstadoPerfilAmplio.activo, 15)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.asignarInstitucion(15, req(4, null)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Esta institución ya tiene un representante activo asignado");
            verify(supervisorRepository, never()).save(any());
        }

        @Test
        @DisplayName("La misma institución que ya representa → 409")
        void mismaInstitucion() {
            Usuario rep = usuario(15, RolUsuario.supervisor, EstadoUsuario.activo);
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(rep));
            when(institucionRepository.findById(8)).thenReturn(Optional.of(inst(8, "activa")));
            when(supervisorRepository.findByUsuario_Id(15)).thenReturn(Optional.of(
                    SupervisorInstitucion.builder().usuario(rep).institucion(inst(8, "activa")).estado(EstadoPerfilAmplio.activo).build()));

            assertThatThrownBy(() -> usuarioService.asignarInstitucion(15, req(8, null)))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("El representante ya está asignado a esa institución");
        }

        @Test
        @DisplayName("Usuario o institución inexistentes → 404")
        void inexistentes() {
            when(usuarioRepository.findById(99)).thenReturn(Optional.empty());
            when(usuarioRepository.findById(15)).thenReturn(Optional.of(usuario(15, RolUsuario.supervisor, EstadoUsuario.activo)));
            when(institucionRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.asignarInstitucion(99, req(8, null)))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("Usuario no encontrado");
            assertThatThrownBy(() -> usuarioService.asignarInstitucion(15, req(999, null)))
                    .isInstanceOf(RecursoNoEncontradoException.class).hasMessage("La institución indicada no existe");
        }
    }
}
