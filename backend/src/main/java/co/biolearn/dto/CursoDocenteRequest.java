package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Datos académicos de un curso que el docente propone o edita. El precio, el estado, las instituciones y los horarios son del Administrador.
 *  Al crear: nombre, idArea e idGrado son obligatorios (se valida en el servicio); al editar, solo se cambian los campos enviados. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoDocenteRequest {

    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    private Integer idArea;

    private Integer idGrado;

    private String descripcion;

    private String objetivos;

    @Size(max = 500, message = "La URL de la portada no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$|^/api/archivos/[0-9a-f-]{36}\\.(png|jpg)$", message = "La portada debe ser una URL que empiece por http:// o https://, o una imagen subida a la plataforma")
    private String imagenPortada;

    /** Código visible del curso, por ejemplo GEN-101 (letras, números y guiones). */
    @Size(max = 20, message = "El código no puede superar 20 caracteres")
    @Pattern(regexp = "^$|^[A-Za-z0-9-]+$", message = "El código solo admite letras, números y guiones")
    private String codigo;

    /** Color representativo en formato #RRGGBB. */
    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "El color debe tener el formato #RRGGBB")
    private String color;

    /** Información para el estudiante antes de inscribirse (HTML del editor enriquecido). */
    private String infoEstudiante;

    @Min(value = 1, message = "Los créditos deben ser mayores que 0")
    @Max(value = 100, message = "Los créditos no pueden superar 100")
    private Integer creditos;

    @Min(value = 1, message = "Las horas estimadas deben ser mayores que 0")
    @Max(value = 10000, message = "Las horas estimadas no pueden superar 10000")
    private Integer horasEstimadas;
}
