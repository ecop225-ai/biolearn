package co.biolearn.controller;

import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.InscripcionResponse;
import co.biolearn.dto.RevisionPlazosResponse;
import co.biolearn.service.AccesoCursoService;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.InscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Inscritos de un curso con su avance (docentes del curso y Administrador), suspensión de inscripciones y revisión de plazos (Administrador). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Inscripciones (gestión)", description = "Seguimiento de los inscritos y estado de las inscripciones")
public class InscripcionGestionController {

    private final InscripcionService servicio;
    private final AccesoCursoService accesoCursoService;

    @GetMapping("/cursos/{idCurso}/inscripciones")
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN')")
    @Operation(summary = "Inscritos del curso con su avance (docentes del curso y Administrador)")
    public ResponseEntity<List<InscripcionResponse>> inscritos(@PathVariable Integer idCurso, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.inscritos(idCurso, u.getUsuario()));
    }

    @PatchMapping("/inscripciones/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspende o reactiva una inscripción (estado: suspendida o activa)")
    public ResponseEntity<InscripcionResponse> estado(@PathVariable Integer id, @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, request));
    }

    @PostMapping("/inscripciones/revision-plazos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Revisa ya los plazos de acceso: vence los cursos sin terminar, cierra los repasos y envía los avisos de 15 días "
            + "(la misma revisión corre todos los días a la 1:30 a. m.)")
    public ResponseEntity<RevisionPlazosResponse> revisarPlazos() {
        return ResponseEntity.ok(accesoCursoService.revisar(java.time.LocalDateTime.now()));
    }
}
