package co.biolearn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroEstudianteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    /** Opcional: si no se envía, el backend genera uno único a partir de nombre y apellido. */
    @Size(min = 4, max = 60, message = "El nombre de usuario debe tener entre 4 y 60 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = PoliticaContrasena.PATRON, message = PoliticaContrasena.MENSAJE)
    private String contrasena;

    @NotNull(message = "El grado es obligatorio")
    private Integer idGrado;

    @NotNull(message = "El área académica es obligatoria")
    private Integer idArea;

    private String codigoInstitucion;
}
