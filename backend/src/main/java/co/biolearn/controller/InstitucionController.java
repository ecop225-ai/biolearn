package co.biolearn.controller;

import co.biolearn.dto.ActualizarInstitucionRequest;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.CrearInstitucionRequest;
import co.biolearn.dto.InstitucionResponse;
import co.biolearn.service.InstitucionService;
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
 * Gestión de instituciones educativas (exclusivo del Administrador).
 * No existe DELETE: una institución se inactiva o suspende, porque tiene estudiantes,
 * representantes y contratos asociados que deben conservarse.
 */
@RestController
@RequestMapping("/api/instituciones")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Instituciones", description = "Registro y administración de instituciones educativas, cupos y códigos de afiliación (exclusivo del Administrador)")
public class InstitucionController {

    private final InstitucionService institucionService;

    @PostMapping
    @Operation(summary = "Registra una institución; el código de afiliación de 8 caracteres lo genera el sistema")
    public ResponseEntity<InstitucionResponse> crear(@Valid @RequestBody CrearInstitucionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institucionService.crear(request));
    }

    @GetMapping
    @Operation(summary = "Lista las instituciones con cupos usados y representante activo; filtros opcionales por estado y búsqueda (nombre, NIT o código)")
    public ResponseEntity<List<InstitucionResponse>> listar(@RequestParam(required = false) String estado,
                                                            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(institucionService.listar(estado, busqueda));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta el detalle de una institución")
    public ResponseEntity<InstitucionResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(institucionService.obtener(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita los datos de una institución (los cupos no pueden quedar por debajo de los estudiantes afiliados)")
    public ResponseEntity<InstitucionResponse> actualizar(@PathVariable Integer id,
                                                          @Valid @RequestBody ActualizarInstitucionRequest request) {
        return ResponseEntity.ok(institucionService.actualizar(id, request));
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de la institución: activa, inactiva o suspendida")
    public ResponseEntity<InstitucionResponse> cambiarEstado(@PathVariable Integer id,
                                                             @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(institucionService.cambiarEstado(id, request));
    }

    @PostMapping("/{id}/regenerar-codigo")
    @Operation(summary = "Genera un nuevo código de afiliación; el anterior deja de funcionar de inmediato")
    public ResponseEntity<InstitucionResponse> regenerarCodigo(@PathVariable Integer id) {
        return ResponseEntity.ok(institucionService.regenerarCodigo(id));
    }
}
