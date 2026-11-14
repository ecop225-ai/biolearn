package co.biolearn.exception;

/** Intento de iniciar sesión con una cuenta en estado 'pendiente' (aún no activada por correo o invitación). */
public class CuentaNoVerificadaException extends RuntimeException {
    public CuentaNoVerificadaException(String mensaje) {
        super(mensaje);
    }
}
