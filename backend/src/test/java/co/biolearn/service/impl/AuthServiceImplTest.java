package co.biolearn.service.impl;

import co.biolearn.dto.CompletarInvitacionRequest;
import co.biolearn.dto.LoginRequest;
import co.biolearn.dto.LoginResponse;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.RecuperarPasswordRequest;
import co.biolearn.dto.ReenviarVerificacionRequest;
import co.biolearn.dto.RegistroEstudianteRequest;
import co.biolearn.dto.RestablecerPasswordRequest;
import co.biolearn.dto.UsuarioResponse;
import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.enums.EstadoToken;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.CredencialesInvalidasException;
import co.biolearn.exception.CuentaNoVerificadaException;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.AreaAcademica;
import co.biolearn.model.Grado;
import co.biolearn.model.Institucion;
import co.biolearn.model.PerfilEstudiante;
import co.biolearn.model.RecuperacionContrasena;
import co.biolearn.model.Usuario;
import co.biolearn.model.VerificacionCuenta;
import co.biolearn.repository.AreaAcademicaRepository;
import co.biolearn.repository.GradoRepository;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.repository.RecuperacionContrasenaRepository;
import co.biolearn.repository.UsuarioRepository;
import co.biolearn.security.JwtUtil;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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
@DisplayName("AuthServiceImpl — módulo de Autenticación")
class AuthServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private InstitucionRepository institucionRepository;
    @Mock private GradoRepository gradoRepository;
    @Mock private AreaAcademicaRepository areaAcademicaRepository;
    @Mock private RecuperacionContrasenaRepository recuperacionRepository;
    @Mock private TokenVerificacionService tokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void configurarComunes() {
        // save() devuelve el mismo objeto que recibe, como haría JPA
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(perfilEstudianteRepository.save(any(PerfilEstudiante.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(gradoRepository.findById(any())).thenReturn(Optional.of(new Grado()));
        when(areaAcademicaRepository.findById(any())).thenReturn(Optional.of(new AreaAcademica()));
    }

    private Usuario usuario(RolUsuario rol, EstadoUsuario estado) {
        return Usuario.builder().id(5).nombre("Sofía").apellido("López").nombreUsuario("sofia.lopez")
                .correo("sofia.lopez@test.com").tipoUsuario(rol).estado(estado).build();
    }

    private RegistroEstudianteRequest registro(String codigoInstitucion) {
        RegistroEstudianteRequest r = new RegistroEstudianteRequest();
        r.setNombre("Sofía");
        r.setApellido("López");
        r.setCorreo("Sofia.Lopez@Test.com");
        r.setContrasena("Clave1234");
        r.setIdGrado(1);
        r.setIdArea(1);
        r.setCodigoInstitucion(codigoInstitucion);
        return r;
    }

    private Institucion institucion(String estado, Integer cupos) {
        return Institucion.builder().id(4).nombre("Colegio San Rafael").codigoAfiliacion("SR7K2MQX")
                .estado(estado).cuposContratados(cupos).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Inicio de sesión")
    class Login {

        private LoginRequest login() {
            LoginRequest r = new LoginRequest();
            r.setUsuario("sofia.lopez@test.com");
            r.setContrasena("Clave1234");
            return r;
        }

        @Test
        @DisplayName("Cuenta activa con credenciales correctas → devuelve token JWT")
        void loginCorrecto() {
            when(usuarioRepository.findByCorreoOrNombreUsuario(anyString(), anyString()))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.activo)));
            when(jwtUtil.generarToken(any(), anyInt(), anyString())).thenReturn("jwt-de-prueba");

            LoginResponse respuesta = authService.login(login());

            assertThat(respuesta.getToken()).isEqualTo("jwt-de-prueba");
            assertThat(respuesta.getTipo()).isEqualTo("Bearer");
        }

        @Test
        @DisplayName("Credenciales incorrectas → CredencialesInvalidasException (401)")
        void credencialesIncorrectas() {
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

            assertThatThrownBy(() -> authService.login(login()))
                    .isInstanceOf(CredencialesInvalidasException.class)
                    .hasMessage("Usuario o contraseña incorrectos");
        }

        @Test
        @DisplayName("Cuenta pendiente → CuentaNoVerificadaException (403 CUENTA_NO_VERIFICADA)")
        void cuentaPendiente() {
            when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("x"));
            when(usuarioRepository.findByCorreoOrNombreUsuario(anyString(), anyString()))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.pendiente)));

            assertThatThrownBy(() -> authService.login(login()))
                    .isInstanceOf(CuentaNoVerificadaException.class)
                    .hasMessageContaining("aún no está activada");
            verify(jwtUtil, never()).generarToken(any(), anyInt(), anyString());
        }

        @Test
        @DisplayName("Cuenta inactiva → se propaga DisabledException (403 cuenta desactivada)")
        void cuentaInactiva() {
            when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("x"));
            when(usuarioRepository.findByCorreoOrNombreUsuario(anyString(), anyString()))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.inactivo)));

            assertThatThrownBy(() -> authService.login(login())).isInstanceOf(DisabledException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Registro de estudiante")
    class Registro {

        @Test
        @DisplayName("Registro independiente → cuenta pendiente, correo en minúsculas, contraseña cifrada y token de 24 h")
        void registroIndependiente() {
            UsuarioResponse respuesta = authService.registrarEstudiante(registro(null));

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario guardado = captor.getValue();
            assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.pendiente);
            assertThat(guardado.getTipoUsuario()).isEqualTo(RolUsuario.estudiante);
            assertThat(guardado.getCorreo()).isEqualTo("sofia.lopez@test.com");
            assertThat(guardado.getContrasena()).isNotEqualTo("Clave1234");
            assertThat(respuesta.getEstado()).isEqualTo(EstadoUsuario.pendiente);
            verify(tokenService).crearTokenActivacion(any(Usuario.class),
                    eq(TokenVerificacionService.HORAS_VERIFICACION_ESTUDIANTE), anyString());
        }

        @Test
        @DisplayName("Sin nombre de usuario → se genera sin tildes: \"sofia.lopez\"")
        void generaNombreUsuario() {
            UsuarioResponse respuesta = authService.registrarEstudiante(registro(null));
            assertThat(respuesta.getNombreUsuario()).isEqualTo("sofia.lopez");
        }

        @Test
        @DisplayName("Nombre de usuario generado ya existe → se agregan 3 dígitos")
        void nombreUsuarioRepetido() {
            when(usuarioRepository.existsByNombreUsuario("sofia.lopez")).thenReturn(true);

            UsuarioResponse respuesta = authService.registrarEstudiante(registro(null));

            assertThat(respuesta.getNombreUsuario()).matches("sofia\\.lopez\\d{3}");
        }

        @Test
        @DisplayName("Correo ya registrado → 409 y no se crea nada")
        void correoDuplicado() {
            when(usuarioRepository.existsByCorreo("sofia.lopez@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registrarEstudiante(registro(null)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ya existe una cuenta registrada con ese correo");
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Con institución activa y cupos libres → el perfil queda afiliado")
        void registroConInstitucion() {
            when(institucionRepository.findByCodigoAfiliacionIgnoreCase("SR7K2MQX"))
                    .thenReturn(Optional.of(institucion("activa", 120)));
            when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(10L);

            authService.registrarEstudiante(registro("SR7K2MQX"));

            ArgumentCaptor<PerfilEstudiante> captor = ArgumentCaptor.forClass(PerfilEstudiante.class);
            verify(perfilEstudianteRepository).save(captor.capture());
            assertThat(captor.getValue().getInstitucion().getNombre()).isEqualTo("Colegio San Rafael");
        }

        @Test
        @DisplayName("Código de institución inexistente → 409")
        void codigoInvalido() {
            when(institucionRepository.findByCodigoAfiliacionIgnoreCase(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registrarEstudiante(registro("XXXX1234")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("no es válido");
        }

        @Test
        @DisplayName("Institución inactiva → 409")
        void institucionInactiva() {
            when(institucionRepository.findByCodigoAfiliacionIgnoreCase(anyString()))
                    .thenReturn(Optional.of(institucion("inactiva", 120)));

            assertThatThrownBy(() -> authService.registrarEstudiante(registro("SR7K2MQX")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Esta institución no está activa en este momento");
        }

        @Test
        @DisplayName("Institución sin cupos → 409 y no se crea la cuenta")
        void sinCupos() {
            when(institucionRepository.findByCodigoAfiliacionIgnoreCase(anyString()))
                    .thenReturn(Optional.of(institucion("activa", 40)));
            when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(40L);

            assertThatThrownBy(() -> authService.registrarEstudiante(registro("SR7K2MQX")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("no tiene cupos disponibles");
            verify(usuarioRepository, never()).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Cupos NULL → sin límite, ni siquiera se cuentan estudiantes")
        void cuposSinLimite() {
            when(institucionRepository.findByCodigoAfiliacionIgnoreCase(anyString()))
                    .thenReturn(Optional.of(institucion("activa", null)));

            assertThat(authService.validarCodigoInstitucion("SR7K2MQX").getNombre()).isEqualTo("Colegio San Rafael");
            verify(perfilEstudianteRepository, never()).countByInstitucion_IdAndEstado(any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Activación de cuentas")
    class Activacion {

        private VerificacionCuenta tokenDe(Usuario u) {
            return VerificacionCuenta.builder().token("tok").usuario(u).build();
        }

        @Test
        @DisplayName("Verificar cuenta de estudiante → queda activa y el token se marca usado")
        void verificarEstudiante() {
            Usuario u = usuario(RolUsuario.estudiante, EstadoUsuario.pendiente);
            VerificacionCuenta v = tokenDe(u);
            when(tokenService.obtenerValido("tok")).thenReturn(v);

            authService.verificarCuenta("tok");

            assertThat(u.getEstado()).isEqualTo(EstadoUsuario.activo);
            verify(tokenService).marcarUsado(v);
        }

        @Test
        @DisplayName("Token de invitación en /verificar-cuenta → rechazado (no activa sin contraseña)")
        void verificarConTokenDeInvitacion() {
            Usuario docente = usuario(RolUsuario.docente, EstadoUsuario.pendiente);
            when(tokenService.obtenerValido("tok")).thenReturn(tokenDe(docente));

            assertThatThrownBy(() -> authService.verificarCuenta("tok"))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("corresponde a una invitación");
            assertThat(docente.getEstado()).isEqualTo(EstadoUsuario.pendiente);
            verify(tokenService, never()).marcarUsado(any());
        }

        @Test
        @DisplayName("Completar invitación de docente → activa y guarda la contraseña cifrada")
        void completarInvitacion() {
            Usuario docente = usuario(RolUsuario.docente, EstadoUsuario.pendiente);
            when(tokenService.obtenerValido("tok")).thenReturn(tokenDe(docente));
            CompletarInvitacionRequest r = new CompletarInvitacionRequest();
            r.setToken("tok");
            r.setContrasena("Docente2026");

            authService.completarInvitacion(r);

            assertThat(docente.getEstado()).isEqualTo(EstadoUsuario.activo);
            assertThat(docente.getContrasena()).isEqualTo("$2a$10$hash");
        }

        @Test
        @DisplayName("Token de estudiante en /completar-invitacion → rechazado")
        void invitacionConTokenDeEstudiante() {
            when(tokenService.obtenerValido("tok")).thenReturn(tokenDe(usuario(RolUsuario.estudiante, EstadoUsuario.pendiente)));
            CompletarInvitacionRequest r = new CompletarInvitacionRequest();
            r.setToken("tok");
            r.setContrasena("Clave1234");

            assertThatThrownBy(() -> authService.completarInvitacion(r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("cuenta de estudiante");
        }

        @Test
        @DisplayName("Reenviar verificación a estudiante pendiente → genera token nuevo")
        void reenviarAPendiente() {
            when(usuarioRepository.findByCorreo("sofia.lopez@test.com"))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.pendiente)));
            ReenviarVerificacionRequest r = new ReenviarVerificacionRequest();
            r.setCorreo("sofia.lopez@test.com");

            authService.reenviarVerificacion(r);

            verify(tokenService).crearTokenActivacion(any(Usuario.class), anyInt(), anyString());
        }

        @Test
        @DisplayName("Reenviar a cuenta activa o correo inexistente → misma respuesta, sin token")
        void reenviarSinEfecto() {
            when(usuarioRepository.findByCorreo("sofia.lopez@test.com"))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.activo)));
            when(usuarioRepository.findByCorreo("nadie@test.com")).thenReturn(Optional.empty());
            ReenviarVerificacionRequest activa = new ReenviarVerificacionRequest();
            activa.setCorreo("sofia.lopez@test.com");
            ReenviarVerificacionRequest inexistente = new ReenviarVerificacionRequest();
            inexistente.setCorreo("nadie@test.com");

            MensajeResponse r1 = authService.reenviarVerificacion(activa);
            MensajeResponse r2 = authService.reenviarVerificacion(inexistente);

            assertThat(r1.getMensaje()).isEqualTo(r2.getMensaje());
            verify(tokenService, never()).crearTokenActivacion(any(), anyInt(), anyString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Recuperación de contraseña")
    class Recuperacion {

        private RecuperacionContrasena recuperacion(EstadoToken estado, LocalDateTime expira, Usuario u) {
            return RecuperacionContrasena.builder().token("rec").usuario(u).estado(estado).expiraEn(expira).build();
        }

        private RestablecerPasswordRequest restablecer(String contrasena) {
            RestablecerPasswordRequest r = new RestablecerPasswordRequest();
            r.setToken("rec");
            r.setNuevaContrasena(contrasena);
            return r;
        }

        @Test
        @DisplayName("Solicitar con correo registrado → guarda token pendiente de 30 min con la IP de origen")
        void solicitarConCorreoRegistrado() {
            when(usuarioRepository.findByCorreo("sofia.lopez@test.com"))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.activo)));
            RecuperarPasswordRequest r = new RecuperarPasswordRequest();
            r.setCorreo("Sofia.Lopez@Test.com");

            authService.solicitarRecuperacion(r, "127.0.0.1");

            ArgumentCaptor<RecuperacionContrasena> captor = ArgumentCaptor.forClass(RecuperacionContrasena.class);
            verify(recuperacionRepository).save(captor.capture());
            RecuperacionContrasena guardada = captor.getValue();
            assertThat(guardada.getEstado()).isEqualTo(EstadoToken.pendiente);
            assertThat(guardada.getIpSolicitud()).isEqualTo("127.0.0.1");
            assertThat(guardada.getExpiraEn()).isBetween(LocalDateTime.now().plusMinutes(29), LocalDateTime.now().plusMinutes(31));
        }

        @Test
        @DisplayName("Solicitar con correo no registrado → misma respuesta y no se guarda ningún token")
        void solicitarConCorreoNoRegistrado() {
            when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());
            RecuperarPasswordRequest existente = new RecuperarPasswordRequest();
            existente.setCorreo("nadie@test.com");

            MensajeResponse respuesta = authService.solicitarRecuperacion(existente, "127.0.0.1");

            assertThat(respuesta.getMensaje()).startsWith("Si el correo está registrado");
            verify(recuperacionRepository, never()).save(any(RecuperacionContrasena.class));
        }

        @Test
        @DisplayName("Restablecer con token válido → contraseña cifrada y token marcado como usado")
        void restablecerValido() {
            Usuario u = usuario(RolUsuario.estudiante, EstadoUsuario.activo);
            RecuperacionContrasena rec = recuperacion(EstadoToken.pendiente, LocalDateTime.now().plusMinutes(10), u);
            when(recuperacionRepository.findByToken("rec")).thenReturn(Optional.of(rec));

            MensajeResponse respuesta = authService.restablecerPassword(restablecer("Nueva2026"));

            assertThat(respuesta.getMensaje()).isEqualTo("Tu contraseña se actualizó correctamente.");
            assertThat(u.getContrasena()).isEqualTo("$2a$10$hash");
            assertThat(rec.getEstado()).isEqualTo(EstadoToken.usado);
            assertThat(rec.getUsadoEn()).isNotNull();
        }

        @Test
        @DisplayName("Restablecer con token inexistente → 409")
        void restablecerTokenInexistente() {
            when(recuperacionRepository.findByToken("rec")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.restablecerPassword(restablecer("Nueva2026")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("El enlace de recuperación no es válido");
        }

        @Test
        @DisplayName("Restablecer con token ya usado → 409 y la contraseña no cambia")
        void restablecerTokenUsado() {
            Usuario u = usuario(RolUsuario.estudiante, EstadoUsuario.activo);
            u.setContrasena("hash-original");
            when(recuperacionRepository.findByToken("rec"))
                    .thenReturn(Optional.of(recuperacion(EstadoToken.usado, LocalDateTime.now().plusMinutes(10), u)));

            assertThatThrownBy(() -> authService.restablecerPassword(restablecer("Nueva2026")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Este enlace ya fue utilizado o ya no es válido");
            assertThat(u.getContrasena()).isEqualTo("hash-original");
        }

        @Test
        @DisplayName("Restablecer con token vencido → 409 y el token pasa a expirado")
        void restablecerTokenVencido() {
            RecuperacionContrasena rec = recuperacion(EstadoToken.pendiente, LocalDateTime.now().minusMinutes(1),
                    usuario(RolUsuario.estudiante, EstadoUsuario.activo));
            when(recuperacionRepository.findByToken("rec")).thenReturn(Optional.of(rec));

            assertThatThrownBy(() -> authService.restablecerPassword(restablecer("Nueva2026")))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Este enlace expiró. Solicita uno nuevo.");
            assertThat(rec.getEstado()).isEqualTo(EstadoToken.expirado);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Usuario autenticado y casos adicionales de registro")
    class Adicionales {

        @Test
        @DisplayName("/me con usuario existente → devuelve sus datos")
        void usuarioActual() {
            when(usuarioRepository.findByCorreo("sofia.lopez@test.com"))
                    .thenReturn(Optional.of(usuario(RolUsuario.estudiante, EstadoUsuario.activo)));

            UsuarioResponse r = authService.obtenerUsuarioActual("sofia.lopez@test.com");

            assertThat(r.getCorreo()).isEqualTo("sofia.lopez@test.com");
            assertThat(r.getRol()).isEqualTo(RolUsuario.estudiante);
        }

        @Test
        @DisplayName("/me con usuario inexistente → 404")
        void usuarioActualInexistente() {
            when(usuarioRepository.findByCorreo(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.obtenerUsuarioActual("nadie@test.com"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("Usuario no encontrado");
        }

        @Test
        @DisplayName("Registro con nombre de usuario propio y libre → se respeta")
        void nombreUsuarioPropio() {
            RegistroEstudianteRequest r = registro(null);
            r.setNombreUsuario("sofi2026");

            assertThat(authService.registrarEstudiante(r).getNombreUsuario()).isEqualTo("sofi2026");
        }

        @Test
        @DisplayName("Registro con nombre de usuario propio ya ocupado → 409")
        void nombreUsuarioPropioOcupado() {
            RegistroEstudianteRequest r = registro(null);
            r.setNombreUsuario("sofi2026");
            when(usuarioRepository.existsByNombreUsuario("sofi2026")).thenReturn(true);

            assertThatThrownBy(() -> authService.registrarEstudiante(r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ese nombre de usuario ya está en uso, elige otro");
        }

        @Test
        @DisplayName("Registro con grado inexistente → 404")
        void gradoInexistente() {
            RegistroEstudianteRequest r = registro(null);
            r.setIdGrado(99);
            when(gradoRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registrarEstudiante(r))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El grado seleccionado no existe");
        }

        @Test
        @DisplayName("Registro con grado inactivo → 409 (no disponible)")
        void gradoInactivo() {
            when(gradoRepository.findById(any())).thenReturn(Optional.of(Grado.builder().id(1).nombre("Sexto").estado("inactivo").build()));

            assertThatThrownBy(() -> authService.registrarEstudiante(registro(null)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("El grado seleccionado no está disponible");
        }

        @Test
        @DisplayName("Registro con área inactiva → 409 (no disponible)")
        void areaInactiva() {
            when(areaAcademicaRepository.findById(any())).thenReturn(Optional.of(AreaAcademica.builder().id(1).nombre("Ecología").estado("inactivo").build()));

            assertThatThrownBy(() -> authService.registrarEstudiante(registro(null)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("El área académica seleccionada no está disponible");
        }

        @Test
        @DisplayName("Registro con área inexistente → 404")
        void areaInexistente() {
            RegistroEstudianteRequest r = registro(null);
            r.setIdArea(99);
            when(areaAcademicaRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.registrarEstudiante(r))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El área académica seleccionada no existe");
        }
    }
}
