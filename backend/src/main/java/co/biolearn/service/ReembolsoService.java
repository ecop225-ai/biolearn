package co.biolearn.service;

import co.biolearn.dto.*;
import co.biolearn.model.Usuario;

import java.time.LocalDate;
import java.util.List;

/** Pagos, Fase 2: reembolsos con nota crédito. */
public interface ReembolsoService {

    // estudiante
    OpcionesReembolsoResponse opciones(Integer idPago, Usuario estudiante);
    ReembolsoResponse solicitar(SolicitudReembolsoRequest request, Usuario estudiante);
    List<ReembolsoResponse> misReembolsos(Usuario estudiante);
    ReembolsoResponse cancelar(Integer idReembolso, Usuario estudiante);
    NotaCreditoResponse miNotaCredito(Integer idNota, Usuario estudiante);

    // contador
    List<ReembolsoResponse> reembolsos(String estado, String busqueda, LocalDate desde, LocalDate hasta, Usuario contador);
    ReembolsoResponse reembolso(Integer idReembolso, Usuario contador);
    ReembolsoResponse aprobar(Integer idReembolso, Usuario contador);
    ReembolsoResponse rechazar(Integer idReembolso, RechazoPagoRequest request, Usuario contador);
    ReembolsoResponse registrarDevolucion(Integer idReembolso, DevolucionRequest request, Usuario contador);
    List<NotaCreditoResponse> notasCredito(String busqueda, LocalDate desde, LocalDate hasta);
    NotaCreditoResponse notaCredito(Integer idNota);
    NotaCreditoResponse enviarNotaCredito(Integer idNota, Usuario contador);
}
