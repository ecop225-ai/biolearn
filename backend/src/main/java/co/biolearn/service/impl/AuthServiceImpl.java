package co.biolearn.service.impl;

import co.biolearn.dto.CompletarInvitacionRequest;
import co.biolearn.dto.InstitucionPublicaResponse;
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
import co.biolearn.security.CustomUserDetails;
import co.biolearn.security.JwtUtil;
import co.biolearn.service.AuthService;
import co.biolearn.service.TokenVerificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
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
    private final TokenVerificacionService tokenService;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final int MINUTOS_EXPIRACION_RECUPERACION = 30;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsuario(), request.getContrasena())
            );
        } catch (BadCredentialsException ex) {
            throw new CredencialesInvalidasException("Usuario o contraseña incorrectos");
        } catch (DisabledException ex) {
            // DisabledException cubre 'pendiente' e 'inactivo'; se distinguen para dar el mensaje correcto.
            boolean pendiente = usuarioRepository.findByCorreoOrNombreUsuario(request.getUsuario(), request.getUsuario())
                    .map(u -> u.getEstado() == EstadoUsuario.pendiente)
                    .orElse(false);
            if (pendiente) {
                throw new CuentaNoVerificadaException(
                        "Tu cuenta aún no está activada. Usa el enlace de activación que recibiste por correo.");
            }
            throw ex; // inactivo → GlobalExceptionHandler responde 403 "cuenta desactivada"
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
        String correo = request.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new SolicitudInvalidaException("Ya existe una cuenta registrada con ese correo");
        }

        String nombreUsuario;
        if (request.getNombreUsuario() != null && !request.getNombreUsuario().isBlank()) {
            nombreUsuario = request.getNombreUsuario().trim();
            if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
                throw new SolicitudInvalidaException("Ese nombre de usuario ya está en uso, elige otro");
            }
        } else {
            nombreUsuario = generarNombreUsuarioUnico(request.getNombre(), request.getApellido());
        }

        Grado grado = gradoRepository.findById(request.getIdGrado())
                .orElseThrow(() -> new RecursoNoEncontradoException("El grado seleccionado no existe"));
        if ("inactivo".equalsIgnoreCase(grado.getEstado())) {
            throw new SolicitudInvalidaException("El grado seleccionado no está disponible");
        }
        AreaAcademica area = areaAcademicaRepository.findById(request.getIdArea())
                .orElseThrow(() -> new RecursoNoEncontradoException("El área académica seleccionada no existe"));
        if ("inactivo".equalsIgnoreCase(area.getEstado())) {
            throw new SolicitudInvalidaException("El área académica seleccionada no está disponible");
        }

        Institucion institucion = null;
        if (request.getCodigoInstitucion() != null && !request.getCodigoInstitucion().isBlank()) {
            institucion = buscarInstitucionDisponible(request.getCodigoInstitucion());
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre().trim())
                .apellido(request.getApellido().trim())
                .nombreUsuario(nombreUsuario)
                .correo(correo)
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .tipoUsuario(RolUsuario.estudiante)
                .estado(EstadoUsuario.pendiente)
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

        tokenService.crearTokenActivacion(usuario, TokenVerificacionService.HORAS_VERIFICACION_ESTUDIANTE,
                "activación de cuenta");

        return aUsuarioResponse(usuario);
    }

    @Override
    public InstitucionPublicaResponse validarCodigoInstitucion(String codigo) {
        Institucion institucion = buscarInstitucionDisponible(codigo);
        return InstitucionPublicaResponse.builder().nombre(institucion.getNombre()).build();
    }

    /** Código existente + institución activa + cupos disponibles. Usado al validar y al registrar. */
    private Institucion buscarInstitucionDisponible(String codigo) {
        Institucion institucion = institucionRepository.findByCodigoAfiliacionIgnoreCase(codigo.trim())
                .orElseThrow(() -> new SolicitudInvalidaException(
                        "El código de institución no es válido. Verifícalo con tu colegio."));
        if (!"activa".equalsIgnoreCase(institucion.getEstado())) {
            throw new SolicitudInvalidaException("Esta institución no está activa en este momento");
        }
        Integer cupos = institucion.getCuposContratados();
        if (cupos != null) {
            long usados = perfilEstudianteRepository.countByInstitucion_IdAndEstado(institucion.getId(), EstadoEstudiante.activo);
            if (usados >= cupos) {
                throw new SolicitudInvalidaException(
                        "Tu institución no tiene cupos disponibles en este momento. Comunícate con tu colegio.");
            }
        }
        return institucion;
    }

    /** nombre.apellido sin tildes ni símbolos; si ya existe, se agregan 3 dígitos aleatorios. */
    private String generarNombreUsuarioUnico(String nombre, String apellido) {
        String base = normalizar(nombre) + "." + normalizar(apellido);
        if (base.length() < 4) base = base + "user";
        if (base.length() > 50) base = base.substring(0, 50);
        String candidato = base;
        int intentos = 0;
        while (usuarioRepository.existsByNombreUsuario(candidato)) {
            candidato = base + (100 + RANDOM.nextInt(900));
            if (++intentos > 20) candidato = base + UUID.randomUUID().toString().substring(0, 6);
        }
        return candidato;
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.replaceAll("[^a-z0-9]", "");
    }

    @Override
    @Transactional
    public MensajeResponse solicitarRecuperacion(RecuperarPasswordRequest request, String ipSolicitud) {
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
        VerificacionCuenta verificacion = tokenService.obtenerValido(token);
        Usuario usuario = verificacion.getUsuario();

        // Un token de invitación de personal no puede usarse aquí: activaría la cuenta sin que la persona
        // haya definido su contraseña. Ese flujo va por /completar-invitacion.
        if (usuario.getTipoUsuario() != RolUsuario.estudiante) {
            throw new SolicitudInvalidaException(
                    "Este enlace corresponde a una invitación. Actívala definiendo tu contraseña.");
        }

        usuario.setEstado(EstadoUsuario.activo);
        usuarioRepository.save(usuario);
        tokenService.marcarUsado(verificacion);

        return MensajeResponse.builder().mensaje("Tu cuenta quedó verificada. Ya puedes iniciar sesión.").build();
    }

    @Override
    @Transactional
    public MensajeResponse reenviarVerificacion(ReenviarVerificacionRequest request) {
        usuarioRepository.findByCorreo(request.getCorreo().trim().toLowerCase())
                .filter(u -> u.getTipoUsuario() == RolUsuario.estudiante && u.getEstado() == EstadoUsuario.pendiente)
                .ifPresent(u -> tokenService.crearTokenActivacion(u,
                        TokenVerificacionService.HORAS_VERIFICACION_ESTUDIANTE, "activación de cuenta (reenvío)"));

        // Respuesta idéntica exista o no la cuenta, para no revelar qué correos están registrados.
        return MensajeResponse.builder()
                .mensaje("Si hay una cuenta pendiente de activación con ese correo, te enviamos un nuevo enlace.")
                .build();
    }

    @Override
    @Transactional
    public MensajeResponse completarInvitacion(CompletarInvitacionRequest request) {
        VerificacionCuenta verificacion = tokenService.obtenerValido(request.getToken());
        Usuario usuario = verificacion.getUsuario();

        if (usuario.getTipoUsuario() == RolUsuario.estudiante) {
            throw new SolicitudInvalidaException(
                    "Este enlace corresponde a la verificación de una cuenta de estudiante, no a una invitación.");
        }

        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        usuario.setEstado(EstadoUsuario.activo);
        usuarioRepository.save(usuario);
        tokenService.marcarUsado(verificacion);

        return MensajeResponse.builder()
                .mensaje("Tu cuenta quedó activada con la contraseña definida. Ya puedes iniciar sesión.")
                .build();
    }

    @Override
    public UsuarioResponse obtenerUsuarioActual(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return aUsuarioResponse(usuario);
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
