package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.ReembolsoService;
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

/** Reembolsos del estudiante (Módulo de Pagos, Fase 2): cursos reembolsables, solicitud, cancelación y nota crédito. */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ESTUDIANTE')")
@Tag(name = "Estudiante: reembolsos", description = "Reembolso de cursos pagados (8 días y menos del 20 % de avance) y notas crédito")
public class EstudianteReembolsoController {

    private final ReembolsoService servicio;

    @GetMapping("/pagos/{idPago}/reembolso")
    @Operation(summary = "Cursos del pago que se pueden reembolsar, plazo y si se piden los datos de la devolución")
    public ResponseEntity<OpcionesReembolsoResponse> opciones(@PathVariable Integer idPago, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.opciones(idPago, u.getUsuario()));
    }

    @PostMapping("/reembolsos")
    @Operation(summary = "Solicitar el reembolso de uno o varios cursos de un pago (PSE y transferencia: medioDevolucion cuenta_bancaria o nequi con sus datos)")
    public ResponseEntity<ReembolsoResponse> solicitar(@Valid @RequestBody SolicitudReembolsoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.solicitar(request, u.getUsuario()));
    }

    @GetMapping("/reembolsos")
    @Operation(summary = "Mis solicitudes de reembolso")
    public ResponseEntity<List<ReembolsoResponse>> mios(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.misReembolsos(u.getUsuario()));
    }

    @PatchMapping("/reembolsos/{id}/cancelacion")
    @Operation(summary = "Cancelar una solicitud que aún no se ha resuelto (los cursos vuelven a estar disponibles)")
    public ResponseEntity<ReembolsoResponse> cancelar(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.cancelar(id, u.getUsuario()));
    }

    @GetMapping("/notas-credito/{id}")
    @Operation(summary = "Mi nota crédito")
    public ResponseEntity<NotaCreditoResponse> notaCredito(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.miNotaCredito(id, u.getUsuario()));
    }
}
