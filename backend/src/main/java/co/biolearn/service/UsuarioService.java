package co.biolearn.service;

import co.biolearn.dto.ActualizarEstadoUsuarioRequest;
import co.biolearn.dto.ActualizarUsuarioRequest;
import co.biolearn.dto.CrearUsuarioRequest;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.UsuarioAdminResponse;

import java.util.List;

/** Operaciones administrativas sobre usuarios, exclusivas del rol ADMIN. */
public interface UsuarioService {

    UsuarioAdminResponse crearUsuario(CrearUsuarioRequest request);

    /** @param idInstitucion opcional: solo supervisores que la representan y estudiantes afiliados a ella. */
    List<UsuarioAdminResponse> listarUsuarios(String rol, String estado, Integer idInstitucion);

    UsuarioAdminResponse obtenerUsuario(Integer id);

    /** @param idAdminActual id del administrador que hace la petición (no puede cambiar su propio estado). */
    UsuarioAdminResponse actualizarEstado(Integer id, ActualizarEstadoUsuarioRequest request, Integer idAdminActual);

    /** Edita datos de perfil de un usuario ya existente (no toca correo, nombreUsuario ni rol). */
    UsuarioAdminResponse actualizarUsuario(Integer id, ActualizarUsuarioRequest request);

    /** Asigna o cambia la institución de un representante institucional (supervisor) ya existente. */
    UsuarioAdminResponse asignarInstitucion(Integer id, co.biolearn.dto.AsignarInstitucionRequest request);

    /** Genera un nuevo token de invitación para una cuenta de personal que sigue pendiente (el anterior se invalida). */
    MensajeResponse reenviarInvitacion(Integer id);
}
