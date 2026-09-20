package co.biolearn.dto;

import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nunca incluye la contraseña — es lo único que la API expone de un Usuario. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Integer id;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String correo;
    private RolUsuario rol;
    private EstadoUsuario estado;
}
