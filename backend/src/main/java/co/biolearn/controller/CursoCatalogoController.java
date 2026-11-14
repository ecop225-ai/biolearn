package co.biolearn.controller;

import co.biolearn.dto.CursoCatalogoResponse;
import co.biolearn.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo público de cursos: solo muestra cursos activos y no requiere sesión (registrado en SecurityConfig). */
@RestController
@RequestMapping("/api/cursos/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo de cursos", description = "Cursos activos visibles para cualquier visitante (público)")
public class CursoCatalogoController {

    private final CursoService cursoService;

    @GetMapping
    @Operation(summary = "Lista los cursos activos (público); filtros opcionales por área, grado y búsqueda")
    public ResponseEntity<List<CursoCatalogoResponse>> catalogo(@RequestParam(required = false) Integer idArea,
                                                                @RequestParam(required = false) Integer idGrado,
                                                                @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(cursoService.catalogo(idArea, idGrado, busqueda));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle público de un curso activo (404 si no existe o no está publicado)")
    public ResponseEntity<CursoCatalogoResponse> detalle(@PathVariable Integer id) {
        return ResponseEntity.ok(cursoService.detalleCatalogo(id));
    }
}
