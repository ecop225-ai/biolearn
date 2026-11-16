package co.biolearn.service;

import co.biolearn.dto.HistorialReporteResponse;
import co.biolearn.dto.ReporteFinancieroResponse;
import co.biolearn.dto.ReporteRequest;
import co.biolearn.model.Usuario;

import java.util.List;

/** Pagos, Fase 3: reportes financieros por período (ingresos, facturas, reembolsos y general) en PDF y Excel. */
public interface ReporteFinancieroService {

    /** Genera el reporte para la vista imprimible (PDF) y lo registra en el historial. */
    ReporteFinancieroResponse generar(ReporteRequest request, Usuario usuario);

    /** Genera el reporte en Excel (.xlsx) y lo registra en el historial. */
    byte[] excel(ReporteRequest request, Usuario usuario);

    List<HistorialReporteResponse> historial();
}
