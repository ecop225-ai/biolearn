package co.biolearn.controller;

import co.biolearn.dto.AreaRequest;
import co.biolearn.dto.AreaResponse;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.GradoRequest;
import co.biolearn.dto.GradoResponse;
import co.biolearn.service.CatalogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogos de grados y áreas académicas.
 * - GET /api/catalogos/grados y /areas: PÚBLICOS, solo registros activos (los usa el formulario de registro).
 * - /api/catalogos/admin/**: exclusivos del Administrador (incluyen inactivos, crear, editar, cambiar estado).
 */
@RestController
@RequestMapping("/api/catalogos")
@RequiredArgsConstructor
@Tag(name = "Catálogos", description = "Grados y áreas académicas: listados públicos para el registro y administración exclusiva del Administrador")
public class CatalogoController {

    private final CatalogoService catalogoService;

    // ── Públicos ────────────────────────────────────────────────────────────

    @GetMapping("/grados")
    @Operation(summary = "Lista los grados activos, ordenados (público)")
    public ResponseEntity<List<GradoResponse>> gradosActivos() {
        return ResponseEntity.ok(catalogoService.listarGrados(true));
    }

    @GetMapping("/areas")
    @Operation(summary = "Lista las áreas académicas activas (público)")
    public ResponseEntity<List<AreaResponse>> areasActivas() {
        return ResponseEntity.ok(catalogoService.listarAreas(true));
    }

    // ── Administración: grados ──────────────────────────────────────────────

    @GetMapping("/admin/grados")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lista todos los grados, incluidos los inactivos (Administrador)")
    public ResponseEntity<List<GradoResponse>> todosLosGrados() {
        return ResponseEntity.ok(catalogoService.listarGrados(false));
    }

    @GetMapping("/admin/grados/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consulta el detalle de un grado, con los estudiantes que lo tienen asignado (Administrador)")
    public ResponseEntity<GradoResponse> obtenerGrado(@PathVariable Integer id) {
        return ResponseEntity.ok(catalogoService.obtenerGrado(id));
    }

    @PostMapping("/admin/grados")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crea un grado: nombre, nivel (primaria, secundaria o media) y orden son obligatorios (Administrador)")
    public ResponseEntity<GradoResponse> crearGrado(@Valid @RequestBody GradoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearGrado(request));
    }

    @PatchMapping("/admin/grados/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita un grado; solo se actualizan los campos enviados (Administrador)")
    public ResponseEntity<GradoResponse> actualizarGrado(@PathVariable Integer id, @Valid @RequestBody GradoRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarGrado(id, request));
    }

    @PatchMapping("/admin/grados/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activa o inactiva un grado (Administrador)")
    public ResponseEntity<GradoResponse> cambiarEstadoGrado(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(catalogoService.cambiarEstadoGrado(id, request));
    }

    // ── Administración: áreas ───────────────────────────────────────────────

    @GetMapping("/admin/areas")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lista todas las áreas académicas, incluidas las inactivas (Administrador)")
    public ResponseEntity<List<AreaResponse>> todasLasAreas() {
        return ResponseEntity.ok(catalogoService.listarAreas(false));
    }

    @GetMapping("/admin/areas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consulta el detalle de un área académica, con los estudiantes que la tienen asignada (Administrador)")
    public ResponseEntity<AreaResponse> obtenerArea(@PathVariable Integer id) {
        return ResponseEntity.ok(catalogoService.obtenerArea(id));
    }

    @PostMapping("/admin/areas")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crea un área académica (Administrador)")
    public ResponseEntity<AreaResponse> crearArea(@Valid @RequestBody AreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearArea(request));
    }

    @PatchMapping("/admin/areas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Edita un área académica (Administrador)")
    public ResponseEntity<AreaResponse> actualizarArea(@PathVariable Integer id, @Valid @RequestBody AreaRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarArea(id, request));
    }

    @PatchMapping("/admin/areas/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activa o inactiva un área académica (Administrador)")
    public ResponseEntity<AreaResponse> cambiarEstadoArea(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(catalogoService.cambiarEstadoArea(id, request));
    }
}
