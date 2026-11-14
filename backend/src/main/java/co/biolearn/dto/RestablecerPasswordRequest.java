package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RestablecerPasswordRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Pattern(regexp = PoliticaContrasena.PATRON, message = PoliticaContrasena.MENSAJE)
    private String nuevaContrasena;
}
