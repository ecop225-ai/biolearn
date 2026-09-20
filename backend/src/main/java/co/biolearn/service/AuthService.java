package co.biolearn.service;

import co.biolearn.dto.LoginRequest;
import co.biolearn.dto.LoginResponse;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.RecuperarPasswordRequest;
import co.biolearn.dto.RegistroEstudianteRequest;
import co.biolearn.dto.RestablecerPasswordRequest;
import co.biolearn.dto.UsuarioResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UsuarioResponse registrarEstudiante(RegistroEstudianteRequest request);

    MensajeResponse solicitarRecuperacion(RecuperarPasswordRequest request, String ipSolicitud);

    MensajeResponse restablecerPassword(RestablecerPasswordRequest request);

    MensajeResponse verificarCuenta(String token);

    UsuarioResponse obtenerUsuarioActual(String correo);
}
