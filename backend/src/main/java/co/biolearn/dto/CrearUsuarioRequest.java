package co.biolearn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearUsuarioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String nombreUsuario;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    @NotNull(message = "El rol es obligatorio")
    private String rol;

    // Datos comunes opcionales, capturables desde el momento de la creación
    private String telefono;
    private String direccion;
    private String ciudad;
    private String docIdentidad;

    // Específicos de Docente
    private String especialidad;

    // Específico de Contador (opcional; por defecto AUXILIAR)
    private String rolContador;

    // Específicos de Supervisor (obligatorios solo si rol = SUPERVISOR)
    private Integer idInstitucion;
    private String cargo;
}
