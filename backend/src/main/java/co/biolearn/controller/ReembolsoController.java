package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.ReembolsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Reembolsos y notas crédito para el contador (Módulo de Pagos, Fase 2). Consultan todos los contadores;
 * aprueban, rechazan, registran la devolución y reenvían solo el administrador financiero y el auxiliar.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CONTADOR')")
@Tag(name = "Reembolsos y notas crédito (contador)", description = "Revisión de solicitudes de reembolso, devoluciones y notas crédito")
public class ReembolsoController {

    private final ReembolsoService servicio;

    @GetMapping("/reembolsos")
    @Operation(summary = "Solicitudes con filtros: estado (solicitado, aprobado, devuelto, rechazado, cancelado), búsqueda y fechas (AAAA-MM-DD)")
    public ResponseEntity<List<ReembolsoResponse>> reembolsos(@RequestParam(required = false) String estado,
                                                              @RequestParam(required = false) String busqueda,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                              @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.reembolsos(estado, busqueda, desde, hasta, u.getUsuario()));
    }

    @GetMapping("/reembolsos/{id}")
    @Operation(summary = "Detalle de una solicitud (el auditor ve la cuenta enmascarada)")
    public ResponseEntity<ReembolsoResponse> reembolso(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.reembolso(id, u.getUsuario()));
    }

    @PatchMapping("/reembolsos/{id}/aprobacion")
    @Operation(summary = "Aprobar: cursos retirados, nota crédito y devolución automática si fue con tarjeta o Nequi")
    public ResponseEntity<ReembolsoResponse> aprobar(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.aprobar(id, u.getUsuario()));
    }

    @PatchMapping("/reembolsos/{id}/rechazo")
    @Operation(summary = "Rechazar con un motivo (el estudiante lo ve y recupera el acceso a sus cursos)")
    public ResponseEntity<ReembolsoResponse> rechazar(@PathVariable Integer id, @Valid @RequestBody RechazoPagoRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.rechazar(id, request, u.getUsuario()));
    }

    @PatchMapping("/reembolsos/{id}/devolucion")
    @Operation(summary = "Registrar la devolución hecha a la cuenta bancaria o al Nequi del estudiante (referencia y comprobante opcional)")
    public ResponseEntity<ReembolsoResponse> devolucion(@PathVariable Integer id, @Valid @RequestBody DevolucionRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.registrarDevolucion(id, request, u.getUsuario()));
    }

    @GetMapping("/notas-credito")
    @Operation(summary = "Notas crédito con búsqueda (número, factura, estudiante o curso) y fechas")
    public ResponseEntity<List<NotaCreditoResponse>> notas(@RequestParam(required = false) String busqueda,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.notasCredito(busqueda, desde, hasta));
    }

    @GetMapping("/notas-credito/{id}")
    @Operation(summary = "Detalle de una nota crédito")
    public ResponseEntity<NotaCreditoResponse> nota(@PathVariable Integer id) {
        return ResponseEntity.ok(servicio.notaCredito(id));
    }

    @PostMapping("/notas-credito/{id}/correo")
    @Operation(summary = "Reenviar la nota crédito al correo del estudiante")
    public ResponseEntity<NotaCreditoResponse> reenviar(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.enviarNotaCredito(id, u.getUsuario()));
    }
}
