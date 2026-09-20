package co.biolearn.exception;

/** Se lanza ante violaciones de reglas de negocio: correo ya registrado, código de institución inválido, etc. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
