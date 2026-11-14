package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Crear o editar un recurso. Al crear, nombre, url y tipoRecurso son obligatorios (se valida en el servicio). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecursoRequest {

    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    private String descripcion;

    /** Video: URL de YouTube. Otros: URL http(s) o la URL de un archivo subido (/api/archivos/…). */
    @Size(max = 1000, message = "La URL no puede superar 1000 caracteres")
    private String url;

    /** video, documento, imagen, audio, enlace o interactivo. */
    private String tipoRecurso;

    @Min(value = 1, message = "La duración debe ser mayor que 0 minutos")
    @Max(value = 1000, message = "La duración no puede superar 1000 minutos")
    private Integer duracionMin;

    /** basico, intermedio o avanzado (opcional). */
    private String nivelAcademico;
}
