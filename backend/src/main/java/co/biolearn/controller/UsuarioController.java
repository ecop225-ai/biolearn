package co.biolearn.controller;

import co.biolearn.dto.ActualizarEstadoUsuarioRequest;
import co.biolearn.dto.ActualizarUsuarioRequest;
import co.biolearn.dto.CrearUsuarioRequest;
import co.biolearn.dto.MensajeResponse;
import co.biolearn.dto.UsuarioAdminResponse;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestión administrativa de usuarios. Todos los endpoints requieren rol ADMIN.
 * No existe DELETE: "eliminar" un usuario se modela como pasar su estado a 'inactivo'
 * (ver PATCH /{id}/estado), porque un borrado físico violaría las llaves foráneas de
 * cursos, pagos y demás historial ya asociado a ese usuario.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Creación y administración de cuentas de Docente, Contador y Supervisor (exclusivo del Administrador)")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final co.biolearn.service.AfiliacionEstudianteService afiliacionEstudianteService;

    @PostMapping
    @Operation(summary = "Crea la cuenta de un Docente, Contador o Supervisor; queda pendiente hasta que la persona invitada active su contraseña")
    public ResponseEntity<UsuarioAdminResponse> crearUsuario(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioAdminResponse creado = usuarioService.crearUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    @Operation(summary = "Lista los usuarios del sistema, con filtros opcionales por rol y estado")
    public ResponseEntity<List<UsuarioAdminResponse>> listarUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Integer idInstitucion) {
        return ResponseEntity.ok(usuarioService.listarUsuarios(rol, estado, idInstitucion));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta el detalle de un usuario por su identificador")
    public ResponseEntity<UsuarioAdminResponse> obtenerUsuario(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerUsuario(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita los datos de perfil de un usuario existente (no permite cambiar correo, nombre de usuario ni rol)")
    public ResponseEntity<UsuarioAdminResponse> actualizarUsuario(@PathVariable Integer id,
                                                                    @RequestBody ActualizarUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, request));
    }

    @PostMapping("/{id}/reenviar-invitacion")
    @Operation(summary = "Genera una nueva invitación (7 días) para un Docente, Contador o Supervisor que aún no activó su cuenta")
    public ResponseEntity<MensajeResponse> reenviarInvitacion(@PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.reenviarInvitacion(id));
    }

    @PutMapping("/{id}/institucion")
    @Operation(summary = "Asigna o cambia la institución de un representante institucional existente (máximo un representante activo por institución)")
    public ResponseEntity<UsuarioAdminResponse> asignarInstitucion(@PathVariable Integer id,
                                                                   @Valid @RequestBody co.biolearn.dto.AsignarInstitucionRequest request) {
        return ResponseEntity.ok(usuarioService.asignarInstitucion(id, request));
    }

    @PutMapping("/{id}/institucion-estudiante")
    @Operation(summary = "Asigna, cambia o quita (idInstitucion null) la institución de un estudiante; conserva sus cursos, pagos y certificados")
    public ResponseEntity<co.biolearn.dto.PerfilEstudianteResponse> institucionEstudiante(@PathVariable Integer id,
                                                                                       @RequestBody co.biolearn.dto.InstitucionEstudianteRequest request,
                                                                                       @org.springframework.security.core.annotation.AuthenticationPrincipal co.biolearn.security.CustomUserDetails u) {
        return ResponseEntity.ok(afiliacionEstudianteService.asignarPorAdministrador(id, request.getIdInstitucion(), u.getUsuario()));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Activa, desactiva o suspende la cuenta de un usuario (equivalente a 'eliminar', sin borrar el registro)")
    public ResponseEntity<UsuarioAdminResponse> actualizarEstado(@PathVariable Integer id,
                                                                   @Valid @RequestBody ActualizarEstadoUsuarioRequest request,
                                                                   @AuthenticationPrincipal CustomUserDetails adminActual) {
        return ResponseEntity.ok(usuarioService.actualizarEstado(id, request, adminActual.getUsuario().getId()));
    }
}
