package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Cuerpo genérico { "estado": "..." } para instituciones y catálogos. */
@Data
public class CambiarEstadoRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;
}
