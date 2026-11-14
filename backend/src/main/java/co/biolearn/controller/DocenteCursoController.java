package co.biolearn.controller;

import co.biolearn.dto.CursoDocenteRequest;
import co.biolearn.dto.CursoDocenteResponse;
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

/** Cursos del docente (Fase 2): consultar los propios, proponer un curso en borrador y editar sus datos académicos. */
@RestController
@RequestMapping("/api/docente/cursos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('DOCENTE')")
@Tag(name = "Cursos del docente", description = "Cursos en los que participa el docente y propuesta de cursos nuevos (solo el Administrador publica)")
public class DocenteCursoController {

    private final ContenidoCursoService servicio;

    @GetMapping
    @Operation(summary = "Lista los cursos del docente (titular, colaborador o invitado) con su rol y el avance del contenido")
    public ResponseEntity<List<CursoDocenteResponse>> misCursos(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.misCursos(u.getUsuario()));
    }

    @PostMapping
    @Operation(summary = "Propone un curso: queda en borrador, con el docente como titular y precio 0 (el Administrador lo publica)")
    public ResponseEntity<CursoDocenteResponse> proponer(@Valid @RequestBody CursoDocenteRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.proponerCurso(request, u.getUsuario()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita los datos académicos del curso (solo el titular; el precio es del Administrador)")
    public ResponseEntity<CursoDocenteResponse> actualizar(@PathVariable Integer id, @Valid @RequestBody CursoDocenteRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.actualizarCursoDocente(id, request, u.getUsuario()));
    }
}
