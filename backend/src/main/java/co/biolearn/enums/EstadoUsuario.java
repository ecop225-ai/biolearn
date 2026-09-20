package co.biolearn.enums;

/**
 * Coincide con el ENUM `estado` de la tabla `usuario`.
 * 'pendiente' = cuenta creada pero aún no verificada por correo / no aceptada por invitación.
 */
public enum EstadoUsuario {
    pendiente,
    activo,
    inactivo,
    suspendido
}
