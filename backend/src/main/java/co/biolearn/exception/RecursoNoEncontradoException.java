package co.biolearn.exception;

/** Se lanza cuando un ID referenciado (grado, área, institución...) no existe. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
