package co.biolearn.exception;

/** 403 con un mensaje específico (por ejemplo, un docente que no pertenece al curso). */
public class SinPermisoException extends RuntimeException {
    public SinPermisoException(String mensaje) { super(mensaje); }
}
