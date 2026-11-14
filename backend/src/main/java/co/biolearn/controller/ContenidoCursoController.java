package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.ContenidoCursoService;
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
 * Contenido de los cursos (Fase 2): lecciones, materiales y recursos.
 * Lectura: docentes del curso (cualquier rol) y Administrador. Edición: titular y colaboradores.
 * Nada se borra: se cambia el estado.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contenido de cursos", description = "Lecciones, materiales y recursos de un curso")
public class ContenidoCursoController {

    private final ContenidoCursoService servicio;

    // ── lecciones ──
    @GetMapping("/cursos/{idCurso}/lecciones")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    @Operation(summary = "Lista las lecciones del curso en orden (docentes del curso y Administrador)")
    public ResponseEntity<List<LeccionResponse>> listar(@PathVariable Integer idCurso, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.listarLecciones(idCurso, u.getUsuario()));
    }

    @PostMapping("/cursos/{idCurso}/lecciones")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Crea una lección en borrador al final del orden (titular y colaboradores)")
    public ResponseEntity<LeccionResponse> crear(@PathVariable Integer idCurso, @Valid @RequestBody LeccionRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearLeccion(idCurso, request, u.getUsuario()));
    }

    @PutMapping("/cursos/{idCurso}/lecciones/orden")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Reordena las lecciones: envía los ids de todas las lecciones no archivadas en el nuevo orden")
    public ResponseEntity<List<LeccionResponse>> reordenar(@PathVariable Integer idCurso, @Valid @RequestBody OrdenLeccionesRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.reordenarLecciones(idCurso, request, u.getUsuario()));
    }

    @GetMapping("/lecciones/{id}")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    @Operation(summary = "Detalle de la lección con sus materiales y recursos (vista previa)")
    public ResponseEntity<LeccionResponse> detalle(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.detalleLeccion(id, u.getUsuario()));
    }

    @PatchMapping("/lecciones/{id}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Edita la lección (idPrerequisito = 0 quita el prerrequisito)")
    public ResponseEntity<LeccionResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody LeccionRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.actualizarLeccion(id, request, u.getUsuario()));
    }

    @PatchMapping("/lecciones/{id}/estado")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Cambia el estado: activa (exige material activo), inactiva, archivada o borrador")
    public ResponseEntity<LeccionResponse> estadoLeccion(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.cambiarEstadoLeccion(id, request, u.getUsuario()));
    }

    // ── materiales ──
    @PostMapping("/lecciones/{id}/materiales")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Agrega un material a la lección (lectura, video, laboratorio, juego, documento o articulo)")
    public ResponseEntity<LeccionResponse> crearMaterial(@PathVariable Integer id, @Valid @RequestBody MaterialRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearMaterial(id, request, u.getUsuario()));
    }

    @PatchMapping("/materiales/{id}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Edita un material")
    public ResponseEntity<LeccionResponse> actualizarMaterial(@PathVariable Integer id, @Valid @RequestBody MaterialRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.actualizarMaterial(id, request, u.getUsuario()));
    }

    @PatchMapping("/materiales/{id}/estado")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Cambia el estado del material: activo, inactivo o archivado")
    public ResponseEntity<LeccionResponse> estadoMaterial(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.cambiarEstadoMaterial(id, request, u.getUsuario()));
    }

    // ── recursos ──
    @PostMapping("/materiales/{id}/recursos")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Agrega un recurso al material (los videos deben ser de YouTube y se convierten al formato para embeber)")
    public ResponseEntity<LeccionResponse> crearRecurso(@PathVariable Integer id, @Valid @RequestBody RecursoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearRecurso(id, request, u.getUsuario()));
    }

    @PatchMapping("/recursos/{id}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Edita un recurso")
    public ResponseEntity<LeccionResponse> actualizarRecurso(@PathVariable Integer id, @Valid @RequestBody RecursoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.actualizarRecurso(id, request, u.getUsuario()));
    }

    @PatchMapping("/recursos/{id}/estado")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Cambia el estado del recurso: activo o inactivo")
    public ResponseEntity<LeccionResponse> estadoRecurso(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.cambiarEstadoRecurso(id, request, u.getUsuario()));
    }
}
