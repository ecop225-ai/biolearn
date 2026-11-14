package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.CursoService;
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
 * Gestión de cursos del Administrador (Fase 1 del módulo de Cursos).
 * No existe borrado físico: un curso se archiva con PATCH /{id}/estado.
 * Los ids de docente que reciben y devuelven estos endpoints son ids de USUARIO (los de la Gestión de usuarios).
 */
@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Cursos", description = "Gestión de cursos: datos, estados, docentes, horarios e instituciones (exclusivo del Administrador)")
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    @Operation(summary = "Lista los cursos; filtros opcionales por estado, área, grado, docente titular (id de usuario) y búsqueda sin tildes")
    public ResponseEntity<List<CursoResponse>> listar(@RequestParam(required = false) String estado,
                                                      @RequestParam(required = false) Integer idArea,
                                                      @RequestParam(required = false) Integer idGrado,
                                                      @RequestParam(required = false) Integer idDocente,
                                                      @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(cursoService.listar(estado, idArea, idGrado, idDocente, busqueda));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta el detalle de un curso con sus docentes, horarios, instituciones y lo que le falta para publicarse")
    public ResponseEntity<CursoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(cursoService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crea un curso en borrador; nombre, área y grado son obligatorios y el docente titular es opcional")
    public ResponseEntity<CursoResponse> crear(@Valid @RequestBody CrearCursoRequest request,
                                               @AuthenticationPrincipal CustomUserDetails admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cursoService.crear(request, admin != null ? admin.getUsuario().getId() : null));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita un curso (solo los campos enviados); incluye el cambio de docente titular. No aplica a cursos archivados")
    public ResponseEntity<CursoResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody ActualizarCursoRequest request) {
        return ResponseEntity.ok(cursoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado: publicar (activo), cerrar, reabrir, archivar o restaurar (borrador)")
    public ResponseEntity<CursoResponse> cambiarEstado(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(cursoService.cambiarEstado(id, request));
    }

    @PostMapping("/{id}/docentes")
    @Operation(summary = "Agrega un docente colaborador o invitado al curso")
    public ResponseEntity<CursoResponse> agregarDocente(@PathVariable Integer id, @Valid @RequestBody AgregarDocenteCursoRequest request) {
        return ResponseEntity.ok(cursoService.agregarDocente(id, request));
    }

    @DeleteMapping("/{id}/docentes/{idDocente}")
    @Operation(summary = "Retira a un docente colaborador o invitado (queda inactivo en el historial); el titular no se retira aquí")
    public ResponseEntity<CursoResponse> retirarDocente(@PathVariable Integer id, @PathVariable Integer idDocente) {
        return ResponseEntity.ok(cursoService.retirarDocente(id, idDocente));
    }

    @PutMapping("/{id}/horarios")
    @Operation(summary = "Define los horarios del curso; reemplaza la lista completa (horas en formato HH:mm, sin cruces el mismo día)")
    public ResponseEntity<CursoResponse> definirHorarios(@PathVariable Integer id, @Valid @RequestBody HorariosCursoRequest request) {
        return ResponseEntity.ok(cursoService.definirHorarios(id, request));
    }

    @PostMapping("/{id}/instituciones")
    @Operation(summary = "Registra que una institución activa contrata el curso")
    public ResponseEntity<CursoResponse> contratar(@PathVariable Integer id, @Valid @RequestBody ContratarCursoRequest request) {
        return ResponseEntity.ok(cursoService.contratar(id, request));
    }

    @DeleteMapping("/{id}/instituciones/{idInstitucion}")
    @Operation(summary = "Quita la contratación del curso para una institución")
    public ResponseEntity<CursoResponse> quitarContratacion(@PathVariable Integer id, @PathVariable Integer idInstitucion) {
        return ResponseEntity.ok(cursoService.quitarContratacion(id, idInstitucion));
    }

    @PostMapping("/{id}/instituciones/{idInstitucion}/codigo")
    @io.swagger.v3.oas.annotations.Operation(summary = "Genera un código de inscripción nuevo para la contratación (Fase 3)")
    public ResponseEntity<CursoResponse> regenerarCodigo(@PathVariable Integer id, @PathVariable Integer idInstitucion) {
        return ResponseEntity.ok(cursoService.regenerarCodigo(id, idInstitucion));
    }
}
