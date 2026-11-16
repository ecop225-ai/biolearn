package co.biolearn.service;

import co.biolearn.dto.PerfilEstudianteResponse;
import co.biolearn.dto.UnirseInstitucionRequest;
import co.biolearn.model.Usuario;

/**
 * Institución del estudiante: unirse, cambiar o salir desde Mi perfil, y el cambio que hace el Administrador.
 * El estudiante conserva siempre sus cursos, su progreso, sus pagos y sus certificados.
 */
public interface AfiliacionEstudianteService {
    /** Unirse a un colegio o cambiar al de ese código (el código prueba que el nuevo colegio lo autoriza). */
    PerfilEstudianteResponse unirse(UnirseInstitucionRequest request, Usuario usuario);

    /** Salir del colegio y quedar como estudiante independiente. */
    PerfilEstudianteResponse salir(Usuario usuario);

    /** El Administrador asigna, cambia o quita (idInstitucion null) la institución de un estudiante. */
    PerfilEstudianteResponse asignarPorAdministrador(Integer idUsuario, Integer idInstitucion, Usuario administrador);
}
