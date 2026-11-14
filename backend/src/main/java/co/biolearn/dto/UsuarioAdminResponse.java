package co.biolearn.dto;

import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAdminResponse {
    private Integer id;
    private String nombre;
    private String apellido;
    private String nombreUsuario;
    private String correo;
    private RolUsuario rol;
    private EstadoUsuario estado;
    private LocalDate fechaRegistro;

    // Datos comunes, antes ausentes de esta respuesta
    private String telefono;
    private String direccion;
    private String ciudad;
    private String docIdentidad;

    // Específicos de rol — solo uno de los tres viene lleno, según el rol del usuario
    private String especialidad;   // Docente
    private String rolContador;    // Contador
    private String cargo;          // Supervisor

    // Institución: la que representa un Supervisor, o a la que está afiliado un Estudiante (null = independiente)
    private Integer idInstitucion;
    private String institucion;
    /** Solo Supervisor: estado de su vínculo en supervisor_institucion (activo / inactivo / suspendido). */
    private String estadoRepresentacion;
}
