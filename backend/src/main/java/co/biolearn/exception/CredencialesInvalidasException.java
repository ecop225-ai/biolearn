package co.biolearn.exception;

/** Se lanza cuando el usuario/correo no existe o la contraseña no coincide. */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
