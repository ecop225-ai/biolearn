package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.PagoService;
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

/** Pagos del estudiante (Módulo de Pagos, Fase 1): carrito, checkout, pasarela de pruebas, comprobante, mis pagos y facturas. */
@RestController
@RequestMapping("/api/estudiante")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ESTUDIANTE')")
@Tag(name = "Estudiante: pagos", description = "Carrito, pago en línea o por transferencia, mis pagos y facturas")
public class EstudiantePagoController {

    private final PagoService servicio;

    @GetMapping("/carrito")
    @Operation(summary = "Cursos pendientes de pago listos para pagar, con el total, y los que ya tienen un pago en proceso")
    public ResponseEntity<CarritoResponse> carrito(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.carrito(u.getUsuario()));
    }

    @PostMapping("/pagos")
    @Operation(summary = "Crea el pago de los cursos elegidos (medio: tarjeta, pse, nequi o transferencia); queda pendiente")
    public ResponseEntity<PagoResponse> crear(@Valid @RequestBody CrearPagoRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crearPago(request, u.getUsuario()));
    }

    @PostMapping("/pagos/{id}/pasarela")
    @Operation(summary = "Paga en la pasarela de pruebas. Tarjeta 4242424242424242 aprueba y 4111111111111111 rechaza; "
            + "Nequi 3991111111 aprueba y 3992222222 rechaza; PSE banco 1 aprueba y 2 rechaza")
    public ResponseEntity<PagoResponse> pasarela(@PathVariable Integer id, @RequestBody(required = false) PagoPasarelaRequest datos,
                                                 @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.pagarEnPasarela(id, datos, u.getUsuario()));
    }

    @PutMapping("/pagos/{id}/comprobante")
    @Operation(summary = "Adjunta el comprobante de una transferencia (URL devuelta por POST /api/archivos)")
    public ResponseEntity<PagoResponse> comprobante(@PathVariable Integer id, @Valid @RequestBody ComprobanteRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.adjuntarComprobante(id, request, u.getUsuario()));
    }

    @PatchMapping("/pagos/{id}/cancelacion")
    @Operation(summary = "Cancela un pago pendiente; los cursos vuelven al carrito")
    public ResponseEntity<PagoResponse> cancelar(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.cancelar(id, u.getUsuario()));
    }

    @GetMapping("/pagos")
    @Operation(summary = "Mis pagos, del más reciente al más antiguo")
    public ResponseEntity<List<PagoResponse>> mios(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.misPagos(u.getUsuario()));
    }

    @GetMapping("/pagos/{id}")
    @Operation(summary = "Detalle de uno de mis pagos")
    public ResponseEntity<PagoResponse> mio(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.miPago(id, u.getUsuario()));
    }

    @GetMapping("/facturas/{id}")
    @Operation(summary = "Una de mis facturas")
    public ResponseEntity<FacturaResponse> factura(@PathVariable Integer id, @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(servicio.miFactura(id, u.getUsuario()));
    }
}
