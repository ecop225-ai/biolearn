package co.biolearn.service.impl;

import co.biolearn.dto.LoginRequest;
import co.biolearn.dto.LoginResponse;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.RecuperarPasswordRequest;
import co.biolearn.dto.RegistroEstudianteRequest;
import co.biolearn.dto.RestablecerPasswordRequest;
import co.biolearn.dto.UsuarioResponse;
import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.enums.EstadoToken;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.enums.TipoVerificacion;
import co.biolearn.exception.CredencialesInvalidasException;
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
import co.biolearn.repository.VerificacionCuentaRepository;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.security.JwtUtil;
import co.biolearn.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final InstitucionRepository institucionRepository;
    private final GradoRepository gradoRepository;
    private final AreaAcademicaRepository areaAcademicaRepository;
    private final RecuperacionContrasenaRepository recuperacionRepository;
    private final VerificacionCuentaRepository verificacionRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final int MINUTOS_EXPIRACION_RECUPERACION = 30;
    private static final int HORAS_EXPIRACION_VERIFICACION = 24;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getContrasena())
            );
        } catch (BadCredentialsException ex) {
            throw new CredencialesInvalidasException("Usuario o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepository.findByCorreoOrNombreUsuario(request.getUsuario(), request.getUsuario())
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contraseña incorrectos"));

        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        String token = jwtUtil.generarToken(userDetails, usuario.getId(), usuario.getTipoUsuario().name());

        return LoginResponse.builder()
                .token(token)
                .tipo("Bearer")
                .usuario(aUsuarioResponse(usuario))
                .build();
    }

    @Override
    @Transactional
    public UsuarioResponse registrarEstudiante(RegistroEstudianteRequest request) {

        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new SolicitudInvalidaException("Ya existe una cuenta registrada con ese correo");
        }
        if (usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new SolicitudInvalidaException("Ese nombre de usuario ya está en uso, elige otro");
        }

        Grado grado = gradoRepository.findById(request.getIdGrado())
                .orElseThrow(() -> new RecursoNoEncontradoException("El grado seleccionado no existe"));
        AreaAcademica area = areaAcademicaRepository.findById(request.getIdArea())
                .orElseThrow(() -> new RecursoNoEncontradoException("El área académica seleccionada no existe"));

        // Institución es opcional (estudiante independiente si se deja vacío).
        Institucion institucion = null;
        if (request.getCodigoInstitucion() != null && !request.getCodigoInstitucion().isBlank()) {
            institucion = institucionRepository.findByCodigoAfiliacionIgnoreCase(request.getCodigoInstitucion().trim())
                    .orElseThrow(() -> new SolicitudInvalidaException(
                            "El código de institución no es válido. Verifícalo con tu colegio."));
            if (!"activa".equalsIgnoreCase(institucion.getEstado())) {
                throw new SolicitudInvalidaException("Esta institución no está activa en este momento");
            }
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .nombreUsuario(request.getNombreUsuario().trim())
                .correo(request.getCorreo().trim().toLowerCase())
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .tipoUsuario(RolUsuario.estudiante)
                .estado(EstadoUsuario.pendiente) // queda pendiente hasta verificar correo (flujo futuro)
                .build();
        usuario = usuarioRepository.save(usuario);

        PerfilEstudiante perfil = PerfilEstudiante.builder()
                .usuario(usuario)
                .institucion(institucion)
                .grado(grado)
                .area(area)
                .estado(EstadoEstudiante.activo)
                .build();
        perfilEstudianteRepository.save(perfil);

        crearTokenVerificacion(usuario);

        return aUsuarioResponse(usuario);
    }

    @Override
    @Transactional
    public MensajeResponse solicitarRecuperacion(RecuperarPasswordRequest request, String ipSolicitud) {
        // Por seguridad, siempre respondemos igual exista o no el correo (no revelamos qué correos están registrados).
        var usuarioOpt = usuarioRepository.findByCorreo(request.getCorreo().trim().toLowerCase());
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = UUID.randomUUID().toString();
            RecuperacionContrasena recuperacion = RecuperacionContrasena.builder()
                    .usuario(usuario)
                    .token(token)
                    .estado(EstadoToken.pendiente)
                    .ipSolicitud(ipSolicitud)
                    .expiraEn(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION_RECUPERACION))
                    .build();
            recuperacionRepository.save(recuperacion);

            // TODO: aquí se integraría el envío real de correo. Mientras tanto, se deja en el log
            // para poder probar el flujo completo desde Swagger sin un servicio de correo real.
            log.info("Token de recuperación de contraseña para {} (expira en {} min): {}",
                    usuario.getCorreo(), MINUTOS_EXPIRACION_RECUPERACION, token);
        }
        return MensajeResponse.builder()
                .mensaje("Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.")
                .build();
    }

    @Override
    @Transactional
    public MensajeResponse restablecerPassword(RestablecerPasswordRequest request) {
        RecuperacionContrasena recuperacion = recuperacionRepository.findByToken(request.getToken())
                .orElseThrow(() -> new SolicitudInvalidaException("El enlace de recuperación no es válido"));

        if (recuperacion.getEstado() != EstadoToken.pendiente) {
            throw new SolicitudInvalidaException("Este enlace ya fue utilizado o ya no es válido");
        }
        if (recuperacion.getExpiraEn().isBefore(LocalDateTime.now())) {
            recuperacion.setEstado(EstadoToken.expirado);
            recuperacionRepository.save(recuperacion);
            throw new SolicitudInvalidaException("Este enlace expiró. Solicita uno nuevo.");
        }

        Usuario usuario = recuperacion.getUsuario();
        usuario.setContrasena(passwordEncoder.encode(request.getNuevaContrasena()));
        usuarioRepository.save(usuario);

        recuperacion.setEstado(EstadoToken.usado);
        recuperacion.setUsadoEn(LocalDateTime.now());
        recuperacionRepository.save(recuperacion);

        return MensajeResponse.builder().mensaje("Tu contraseña se actualizó correctamente.").build();
    }

    @Override
    @Transactional
    public MensajeResponse verificarCuenta(String token) {
        VerificacionCuenta verificacion = verificacionRepository.findByToken(token)
                .orElseThrow(() -> new SolicitudInvalidaException("El enlace de verificación no es válido"));

        if (verificacion.getEstado() != EstadoToken.pendiente) {
            throw new SolicitudInvalidaException("Esta cuenta ya fue verificada anteriormente");
        }
        if (verificacion.getExpiraEn().isBefore(LocalDateTime.now())) {
            verificacion.setEstado(EstadoToken.expirado);
            verificacionRepository.save(verificacion);
            throw new SolicitudInvalidaException("El enlace de verificación expiró. Solicita uno nuevo.");
        }

        Usuario usuario = verificacion.getUsuario();
        usuario.setEstado(EstadoUsuario.activo);
        usuarioRepository.save(usuario);

        verificacion.setEstado(EstadoToken.usado);
        verificacion.setUsadoEn(LocalDateTime.now());
        verificacionRepository.save(verificacion);

        return MensajeResponse.builder().mensaje("Tu cuenta quedó verificada. Ya puedes iniciar sesión.").build();
    }

    @Override
    public UsuarioResponse obtenerUsuarioActual(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return aUsuarioResponse(usuario);
    }

    private void crearTokenVerificacion(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        VerificacionCuenta verificacion = VerificacionCuenta.builder()
                .usuario(usuario)
                .token(token)
                .tipo(TipoVerificacion.activacion)
                .estado(EstadoToken.pendiente)
                .expiraEn(LocalDateTime.now().plusHours(HORAS_EXPIRACION_VERIFICACION))
                .build();
        verificacionRepository.save(verificacion);

        // TODO: aquí se integraría el envío real de correo de bienvenida/activación.
        log.info("Token de verificación de cuenta para {} (expira en {} h): {}",
                usuario.getCorreo(), HORAS_EXPIRACION_VERIFICACION, token);
    }

    private UsuarioResponse aUsuarioResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .nombreUsuario(usuario.getNombreUsuario())
                .correo(usuario.getCorreo())
                .rol(usuario.getTipoUsuario())
                .estado(usuario.getEstado())
                .build();
    }
}
