package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El usuario o correo es obligatorio")
    private String usuario; // acepta correo o nombre_usuario, igual que el prototipo

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;
}
