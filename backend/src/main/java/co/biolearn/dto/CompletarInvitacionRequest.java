package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** Usado por Docente/Contador/Supervisor para activar la cuenta creada por el Administrador,
 *  definiendo su propia contraseña a partir del token recibido por correo. */
@Data
public class CompletarInvitacionRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = PoliticaContrasena.PATRON, message = PoliticaContrasena.MENSAJE)
    private String contrasena;
}
