package co.biolearn.exception;

import co.biolearn.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({CredencialesInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> manejarCredencialesInvalidas(RuntimeException ex) {
        return construir(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", null);
    }

    /** Login de una cuenta en estado 'inactivo' (Spring lanza DisabledException). Antes caía en el 500 genérico. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> manejarCuentaInactiva(DisabledException ex) {
        return construir(HttpStatus.FORBIDDEN, "Tu cuenta está desactivada. Comunícate con el administrador.", null);
    }

    /** Login de una cuenta en estado 'suspendido' (Spring lanza LockedException). Antes caía en el 500 genérico. */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> manejarCuentaSuspendida(LockedException ex) {
        return construir(HttpStatus.FORBIDDEN, "Tu cuenta está suspendida. Comunícate con el administrador.", null);
    }

    /** Login de una cuenta 'pendiente'. Incluye un código para que el frontend ofrezca "Activar mi cuenta". */
    @ExceptionHandler(CuentaNoVerificadaException.class)
    public ResponseEntity<ErrorResponse> manejarCuentaNoVerificada(CuentaNoVerificadaException ex) {
        ErrorResponse cuerpo = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estadoHttp(HttpStatus.FORBIDDEN.value())
                .mensaje(ex.getMessage())
                .codigo("CUENTA_NO_VERIFICADA")
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(cuerpo);
    }

    /** 403 con el motivo concreto (por ejemplo, un docente que no pertenece al curso o un invitado que intenta editar). */
    @ExceptionHandler(SinPermisoException.class)
    public ResponseEntity<ErrorResponse> manejarSinPermiso(SinPermisoException ex) {
        return construir(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    /** Archivo que supera el límite del servidor (11 MB) o petición multipart incompleta. */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> manejarArchivoGrande(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El archivo supera el tamaño máximo de 10 MB", null);
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> manejarSinArchivo(org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Adjunta el archivo en el campo «archivo»", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccessDeniedException ex) {
        return construir(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción", null);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorResponse> manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "Hay campos inválidos en la solicitud", detalles);
    }

    /** Valores de enum no reconocidos (ej. ?rol=profesor o rolContador="jefe"). Antes: 500. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Uno de los valores enviados no es válido", List.of(String.valueOf(ex.getMessage())));
    }

    /** JSON mal formado o tipo de dato incorrecto en el cuerpo. Antes: 500. */
    /** Un dato que viola una restricción de la base (NOT NULL, UNIQUE, llave foránea) antes caía en el 500 genérico
     *  y ocultaba la causa. Se registra el detalle técnico en la consola y se responde 409 con un mensaje claro. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridadDatos(org.springframework.dao.DataIntegrityViolationException ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return construir(HttpStatus.CONFLICT, "Los datos enviados no cumplen las reglas de la base de datos", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoIlegible(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato JSON válido", null);
    }

    /** Parámetro de ruta/consulta con tipo incorrecto (ej. /api/usuarios/abc). Antes: 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un valor no válido", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return construir(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", null);
    }

    /** Ruta existente con otro método (por ejemplo, DELETE sobre una ruta que solo acepta PUT): 405, no 500. */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoPermitido(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "El método " + ex.getMethod() + " no está permitido en esta ruta",
                ex.getSupportedMethods() != null ? List.of("Métodos permitidos: " + String.join(", ", ex.getSupportedMethods())) : null);
    }

    /** Ruta inexistente: 404, no 500. */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return construir(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGenerico(Exception ex) {
        // Se registra en consola para poder diagnosticar; al cliente solo le llega el mensaje genérico.
        log.error("Error no controlado", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor", null);
    }

    private ResponseEntity<ErrorResponse> construir(HttpStatus status, String mensaje, List<String> detalles) {
        ErrorResponse cuerpo = ErrorResponse.builder()
                .fecha(LocalDateTime.now())
                .estadoHttp(status.value())
                .mensaje(mensaje)
                .detalles(detalles)
                .build();
        return ResponseEntity.status(status).body(cuerpo);
    }
}
