package co.biolearn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Al crear: nombre, nivel y orden son obligatorios (se valida en el servicio).
 *  Al editar: todos son opcionales y solo se actualiza lo que se envía. */
@Data
public class GradoRequest {

    @Size(min = 1, max = 60, message = "El nombre debe tener entre 1 y 60 caracteres")
    private String nombre;

    /** primaria | secundaria | media */
    private String nivel;

    @Min(value = 1, message = "El orden debe ser mayor que 0")
    @Max(value = 255, message = "El orden admite máximo 255")
    private Integer orden;

    private String descripcion;
}
