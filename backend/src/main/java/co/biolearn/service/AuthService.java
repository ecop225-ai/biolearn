package co.biolearn.service;

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

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UsuarioResponse registrarEstudiante(RegistroEstudianteRequest request);

    MensajeResponse solicitarRecuperacion(RecuperarPasswordRequest request, String ipSolicitud);

    MensajeResponse restablecerPassword(RestablecerPasswordRequest request);

    /** Activa la cuenta de un ESTUDIANTE a partir del token de verificación de correo. */
    MensajeResponse verificarCuenta(String token);

    /** Genera un nuevo enlace de verificación para un estudiante pendiente. Respuesta genérica siempre. */
    MensajeResponse reenviarVerificacion(ReenviarVerificacionRequest request);

    UsuarioResponse obtenerUsuarioActual(String correo);

    /** Activa la cuenta de un Docente/Contador/Supervisor creada por el Administrador,
     *  definiendo la contraseña a partir del token de invitación. */
    MensajeResponse completarInvitacion(CompletarInvitacionRequest request);

    /** Valida un código de afiliación durante el registro y devuelve el nombre de la institución. */
    InstitucionPublicaResponse validarCodigoInstitucion(String codigo);
}
