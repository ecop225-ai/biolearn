package co.biolearn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registro público — solo para ESTUDIANTES.
 * Docente, Contador, Supervisor y Admin no se autorregistran (los crea el Administrador por invitación).
 */
@Data
public class RegistroEstudianteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 60, message = "El nombre de usuario debe tener entre 4 y 60 caracteres")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    private String contrasena;

    @NotNull(message = "El grado es obligatorio")
    private Integer idGrado;

    @NotNull(message = "El área académica es obligatoria")
    private Integer idArea;

    /**
     * Código de afiliación de la institución (ej. SANRAFAEL2026).
     * Se deja vacío/null si el estudiante es independiente.
     */
    private String codigoInstitucion;
}
