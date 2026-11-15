package co.biolearn.controller;

import co.biolearn.dto.FacturaResponse;
import co.biolearn.dto.PagoResponse;
import co.biolearn.dto.PermisosContadorResponse;
import co.biolearn.dto.RechazoPagoRequest;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.PagoService;
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
 * Pagos y facturas para el contador (Módulo de Pagos, Fase 1). Consultan el contador y el Administrador;
 * confirman, rechazan y reenvían solo los contadores administrador financiero y auxiliar.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Pagos y facturas (contador)", description = "Consulta y confirmación de pagos, y facturas emitidas")
public class PagoController {

    private final PagoService servicio;

    @GetMapping("/pagos")
    @PreAuthorize("hasAnyRole('CONTADOR','ADMIN')")
    @Operation(summary = "Pagos con filtros: estado, medio (tarjeta, pse, nequi, transferencia), búsqueda y fechas (AAAA-MM-DD)")
    public ResponseEntity<List<PagoResponse>> pagos(@RequestParam(required = false) String estado,
                                                    @RequestParam(required = false) String medio,
                                                    @RequestParam(required = false) String busqueda,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.pagos(estado, medio, busqueda, desde, hasta));
    }

    @GetMapping("/pagos/permisos")
    @PreAuthorize("hasRole('CONTADOR')")
    @Operation(summary = "Rol del contador que inició sesión y si puede confirmar, rechazar y reenviar (el auditor solo consulta)")
    public ResponseEntity<PermisosContadorResponse> permisos(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.permisos(u.getUsuario()));
    }

    @GetMapping("/pagos/{id}")
    @PreAuthorize("hasAnyRole('CONTADOR','ADMIN')")
    @Operation(summary = "Detalle de un pago con sus cursos y el comprobante")
    public ResponseEntity<PagoResponse> pago(@PathVariable Integer id) {
        return ResponseEntity.ok(servicio.pago(id));
    }

    @PatchMapping("/pagos/{id}/confirmacion")
    @PreAuthorize("hasRole('CONTADOR')")
    @Operation(summary = "Confirma una transferencia con comprobante: activa los cursos y emite la factura")
    public ResponseEntity<PagoResponse> confirmar(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.confirmar(id, u.getUsuario()));
    }

    @PatchMapping("/pagos/{id}/rechazo")
    @PreAuthorize("hasRole('CONTADOR')")
    @Operation(summary = "Rechaza una transferencia con un motivo; los cursos vuelven al carrito del estudiante")
    public ResponseEntity<PagoResponse> rechazar(@PathVariable Integer id, @Valid @RequestBody RechazoPagoRequest request,
                                                 @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.rechazar(id, request, u.getUsuario()));
    }

    @GetMapping("/facturas")
    @PreAuthorize("hasAnyRole('CONTADOR','ADMIN')")
    @Operation(summary = "Facturas emitidas con búsqueda (número, estudiante, curso) y fechas (AAAA-MM-DD)")
    public ResponseEntity<List<FacturaResponse>> facturas(@RequestParam(required = false) String busqueda,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(servicio.facturas(busqueda, desde, hasta));
    }

    @GetMapping("/facturas/{id}")
    @PreAuthorize("hasAnyRole('CONTADOR','ADMIN')")
    @Operation(summary = "Detalle de una factura con los datos del emisor guardados al emitirla")
    public ResponseEntity<FacturaResponse> factura(@PathVariable Integer id) {
        return ResponseEntity.ok(servicio.factura(id));
    }

    @PostMapping("/facturas/{id}/correo")
    @PreAuthorize("hasRole('CONTADOR')")
    @Operation(summary = "Reenvía la factura al correo del estudiante")
    public ResponseEntity<FacturaResponse> correo(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.enviarFactura(id, u.getUsuario()));
    }
}
