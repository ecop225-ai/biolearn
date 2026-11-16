package co.biolearn.controller;

import co.biolearn.dto.*;
import co.biolearn.security.CustomUserDetails;
import co.biolearn.service.FinanzasService;
import co.biolearn.service.ReporteFinancieroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pagos, Fase 3: inicio del contador, datos de facturación y de recaudo, y reportes financieros.
 * Consultan el contador (los tres roles) y el Administrador; editan los datos el Administrador y el administrador financiero.
 */
@RestController
@RequestMapping("/api/finanzas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('CONTADOR','ADMIN')")
@Tag(name = "Finanzas: datos y reportes", description = "Inicio del contador, datos de facturación y recaudo, y reportes financieros")
public class FinanzasController {

    static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final FinanzasService finanzas;
    private final ReporteFinancieroService reportes;

    @GetMapping("/resumen")
    @Operation(summary = "Cifras del mes, pendientes (transferencias por confirmar, reembolsos por revisar y por devolver), últimos 6 meses y movimientos")
    public ResponseEntity<ResumenFinancieroResponse> resumen() {
        return ResponseEntity.ok(finanzas.resumen());
    }

    @GetMapping("/configuracion")
    @Operation(summary = "Datos de facturación y de recaudo vigentes, y si quien consulta puede editarlos")
    public ResponseEntity<ConfiguracionFinancieraResponse> configuracion(@AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(finanzas.configuracion(u.getUsuario()));
    }

    @PutMapping("/datos-facturacion")
    @Operation(summary = "Guarda una versión nueva de los datos del emisor (Administrador y administrador financiero)")
    public ResponseEntity<DatosFacturacionResponse> facturacion(@Valid @RequestBody DatosFacturacionRequest request,
                                                                @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(finanzas.actualizarFacturacion(request, u.getUsuario()));
    }

    @GetMapping("/datos-facturacion/historial")
    @Operation(summary = "Versiones de los datos de facturación, de la vigente a la más antigua")
    public ResponseEntity<List<DatosFacturacionResponse>> historialFacturacion() {
        return ResponseEntity.ok(finanzas.historialFacturacion());
    }

    @PutMapping("/datos-recaudo")
    @Operation(summary = "Guarda una versión nueva de la cuenta y el Nequi para transferencias (Administrador y administrador financiero)")
    public ResponseEntity<DatosRecaudoResponse> recaudo(@Valid @RequestBody DatosRecaudoRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(finanzas.actualizarRecaudo(request, u.getUsuario()));
    }

    @GetMapping("/datos-recaudo/historial")
    @Operation(summary = "Versiones de la cuenta de recaudo, de la vigente a la más antigua")
    public ResponseEntity<List<DatosRecaudoResponse>> historialRecaudo() {
        return ResponseEntity.ok(finanzas.historialRecaudo());
    }

    @PostMapping("/reportes")
    @Operation(summary = "Genera un reporte (ingresos, facturas, reembolsos o general) para la vista imprimible / PDF y lo registra")
    public ResponseEntity<ReporteFinancieroResponse> reporte(@Valid @RequestBody ReporteRequest request,
                                                             @AuthenticationPrincipal CustomUserDetails u) {
        return ResponseEntity.ok(reportes.generar(request, u.getUsuario()));
    }

    @PostMapping("/reportes/excel")
    @Operation(summary = "Genera el reporte en Excel (.xlsx) y lo registra")
    public ResponseEntity<byte[]> excel(@Valid @RequestBody ReporteRequest request, @AuthenticationPrincipal CustomUserDetails u) {
        byte[] archivo = reportes.excel(request, u.getUsuario());
        String nombre = "BioLearn_" + request.getTipo() + "_" + request.getDesde() + "_" + request.getHasta() + ".xlsx";
        return ResponseEntity.ok().contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(archivo);
    }

    @GetMapping("/reportes/historial")
    @Operation(summary = "Últimos 50 reportes generados con su motivo y autor")
    public ResponseEntity<List<HistorialReporteResponse>> historial() {
        return ResponseEntity.ok(reportes.historial());
    }
}
