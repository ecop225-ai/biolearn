package co.biolearn.service;

import co.biolearn.enums.EstadoToken;
import co.biolearn.enums.TipoVerificacion;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Usuario;
import co.biolearn.model.VerificacionCuenta;
import co.biolearn.repository.VerificacionCuentaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lógica única de los tokens de la tabla verificacion_cuenta, compartida por:
 *  - Auth: verificación de correo del estudiante (24 h) y su reenvío.
 *  - Usuarios: invitación de Docente/Contador/Supervisor (7 días) y su reenvío.
 * Antes esta lógica estaba duplicada en AuthServiceImpl y UsuarioServiceImpl.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenVerificacionService {

    public static final int HORAS_VERIFICACION_ESTUDIANTE = 24;
    public static final int HORAS_INVITACION_PERSONAL = 168; // 7 días

    private final VerificacionCuentaRepository verificacionRepository;

    /** Crea un token nuevo de activación. Invalida antes los pendientes del mismo usuario,
     *  para que al reenviar solo el enlace más reciente sea válido. */
    public VerificacionCuenta crearTokenActivacion(Usuario usuario, int horasExpiracion, String etiquetaLog) {
        invalidarPendientes(usuario);

        VerificacionCuenta verificacion = VerificacionCuenta.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .tipo(TipoVerificacion.activacion)
                .estado(EstadoToken.pendiente)
                .expiraEn(LocalDateTime.now().plusHours(horasExpiracion))
                .build();
        verificacionRepository.save(verificacion);

        // Mientras no haya envío real de correos, el token se consulta en la consola del backend.
        log.info("Token de {} para {} (expira en {} h): {}",
                etiquetaLog, usuario.getCorreo(), horasExpiracion, verificacion.getToken());
        return verificacion;
    }

    public void invalidarPendientes(Usuario usuario) {
        verificacionRepository.findByUsuario_IdAndEstado(usuario.getId(), EstadoToken.pendiente)
                .forEach(v -> {
                    v.setEstado(EstadoToken.expirado);
                    verificacionRepository.save(v);
                });
    }

    /** Devuelve el token si existe, está pendiente y no ha expirado; si no, lanza un error controlado. */
    public VerificacionCuenta obtenerValido(String token) {
        VerificacionCuenta verificacion = verificacionRepository.findByToken(token)
                .orElseThrow(() -> new SolicitudInvalidaException("El enlace de activación no es válido"));

        if (verificacion.getEstado() != EstadoToken.pendiente) {
            throw new SolicitudInvalidaException("Este enlace ya fue utilizado o fue reemplazado por uno más reciente");
        }
        if (verificacion.getExpiraEn().isBefore(LocalDateTime.now())) {
            verificacion.setEstado(EstadoToken.expirado);
            verificacionRepository.save(verificacion);
            throw new SolicitudInvalidaException("El enlace expiró. Solicita uno nuevo.");
        }
        return verificacion;
    }

    public void marcarUsado(VerificacionCuenta verificacion) {
        verificacion.setEstado(EstadoToken.usado);
        verificacion.setUsadoEn(LocalDateTime.now());
        verificacionRepository.save(verificacion);
    }
}
