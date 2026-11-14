package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Crear o editar una lección. Al crear, el nombre es obligatorio (se valida en el servicio).
 *  idPrerequisito: id de una lección anterior del mismo curso; al editar, 0 quita el prerrequisito. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeccionRequest {

    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    private String descripcion;

    /** teoria, practica, evaluacion o mixta. */
    private String tipo;

    @Size(max = 500, message = "La URL de la portada no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$|^/api/archivos/[0-9a-f-]{36}\\.(png|jpg)$", message = "La portada debe ser una URL que empiece por http:// o https://, o una imagen subida a la plataforma")
    private String imagenPortada;

    private Integer idPrerequisito;
}
