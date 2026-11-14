package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarEstadoUsuarioRequest {

    @NotBlank(message = "El estado es obligatorio")
    private String estado;
}
