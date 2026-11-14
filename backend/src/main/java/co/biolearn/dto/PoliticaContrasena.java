package co.biolearn.dto;

/** Política única de contraseñas para registro, restablecimiento y activación por invitación.
 *  Debe coincidir con el checklist que muestran los prototipos. */
public final class PoliticaContrasena {
    public static final String PATRON = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
    public static final String MENSAJE = "La contraseña debe tener mínimo 8 caracteres, al menos una mayúscula y al menos un número";

    private PoliticaContrasena() { }
}
