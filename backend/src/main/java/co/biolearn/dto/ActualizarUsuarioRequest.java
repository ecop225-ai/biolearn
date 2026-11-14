package co.biolearn.dto;

import lombok.Data;

/**
 * Edición de un usuario ya existente, exclusiva del Administrador.
 * A propósito NO incluye correo, nombreUsuario ni rol:
 * - correo: cambiarlo sin reverificación sería un hueco de seguridad.
 * - rol: cambiar de rol implica borrar un perfil y crear otro; no es una simple edición.
 * Todos los campos son opcionales — solo se actualiza lo que venga distinto de null.
 */
@Data
public class ActualizarUsuarioRequest {

    private String nombre;
    private String apellido;
    private String telefono;
    private String direccion;
    private String ciudad;
    private String docIdentidad;

    // Específico de Docente
    private String especialidad;

    // Específico de Contador
    private String rolContador;

    // Específico de Supervisor
    private String cargo;
}
