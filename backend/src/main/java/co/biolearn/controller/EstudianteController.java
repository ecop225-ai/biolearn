package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.InscripcionService;
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

/** Portal del estudiante (Fase 3): catálogo, inscripción con o sin código, sus cursos, lecciones y progreso. */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ESTUDIANTE')")
@Tag(name = "Estudiante: cursos y aprendizaje", description = "Catálogo, inscripciones, lecciones y progreso del estudiante")
public class EstudianteController {

    private final InscripcionService servicio;

    @GetMapping("/perfil")
    @Operation(summary = "Mis datos: institución, grado y área")
    public ResponseEntity<PerfilEstudianteResponse> perfil(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.perfil(u.getUsuario()));
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Cursos activos con mi estado de inscripción y la marca «de mi colegio»")
    public ResponseEntity<List<CursoEstudianteResponse>> catalogo(@RequestParam(required = false) String busqueda, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.catalogo(u.getUsuario(), busqueda));
    }

    @GetMapping("/catalogo/{idCurso}")
    @Operation(summary = "Detalle de un curso: información para el estudiante y títulos de sus lecciones")
    public ResponseEntity<CursoEstudianteResponse> detalle(@PathVariable Integer idCurso, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.detalle(idCurso, u.getUsuario()));
    }

    @GetMapping("/codigos/{codigo}")
    @Operation(summary = "Busca el curso de un código de inscripción (solo para estudiantes de esa institución)")
    public ResponseEntity<CursoEstudianteResponse> porCodigo(@PathVariable String codigo, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.buscarPorCodigo(codigo, u.getUsuario()));
    }

    @PostMapping("/inscripciones")
    @Operation(summary = "Inscribirse: un curso gratuito queda activo; uno con precio, pendiente de pago")
    public ResponseEntity<InscripcionResponse> inscribirse(@Valid @RequestBody InscripcionRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.inscribirse(request, u.getUsuario()));
    }

    @GetMapping("/inscripciones")
    @Operation(summary = "Mis inscripciones con su avance")
    public ResponseEntity<List<InscripcionResponse>> mias(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.misInscripciones(u.getUsuario()));
    }

    @PatchMapping("/inscripciones/{id}/retiro")
    @Operation(summary = "Retirarse de un curso (se conserva el progreso si vuelve a inscribirse)")
    public ResponseEntity<InscripcionResponse> retirarse(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.retirarse(id, u.getUsuario()));
    }

    @GetMapping("/cursos/{idCurso}/lecciones")
    @Operation(summary = "Lecciones activas del curso con mi progreso y si están bloqueadas")
    public ResponseEntity<List<LeccionEstudianteResponse>> lecciones(@PathVariable Integer idCurso, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.lecciones(idCurso, u.getUsuario()));
    }

    @GetMapping("/lecciones/{id}")
    @Operation(summary = "Contenido de la lección (materiales y recursos activos); la primera vez queda en progreso")
    public ResponseEntity<LeccionEstudianteResponse> leccion(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.leccion(id, u.getUsuario()));
    }

    @PostMapping("/materiales/{id}/apertura")
    @Operation(summary = "Registra que el estudiante abrió el material y devuelve su tiempo mínimo de revisión")
    public ResponseEntity<AperturaMaterialResponse> apertura(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.registrarApertura(id, u.getUsuario()));
    }

    @PutMapping("/materiales/{id}/visto")
    @Operation(summary = "Marca el material como visto (exige la apertura y su tiempo mínimo) y recalcula el avance")
    public ResponseEntity<LeccionEstudianteResponse> marcar(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.marcarVisto(id, u.getUsuario()));
    }
}
