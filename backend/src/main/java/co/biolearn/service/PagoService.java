package co.biolearn.service;

import co.biolearn.dto.*;
import co.biolearn.model.Usuario;

import java.time.LocalDate;
import java.util.List;

/** Módulo de Pagos, Fase 1: carrito, pago (en línea o transferencia), confirmación y factura. */
public interface PagoService {

    // estudiante
    CarritoResponse carrito(Usuario usuario);
    PagoResponse crearPago(CrearPagoRequest request, Usuario usuario);
    PagoResponse pagarEnPasarela(Integer idPago, PagoPasarelaRequest datos, Usuario usuario);
    PagoResponse adjuntarComprobante(Integer idPago, ComprobanteRequest request, Usuario usuario);
    PagoResponse cancelar(Integer idPago, Usuario usuario);
    List<PagoResponse> misPagos(Usuario usuario);
    PagoResponse miPago(Integer idPago, Usuario usuario);
    FacturaResponse miFactura(Integer idFactura, Usuario usuario);

    // contador
    List<PagoResponse> pagos(String estado, String medio, String busqueda, LocalDate desde, LocalDate hasta);
    PagoResponse pago(Integer idPago);
    PagoResponse confirmar(Integer idPago, Usuario contador);
    PagoResponse rechazar(Integer idPago, RechazoPagoRequest request, Usuario contador);
    List<FacturaResponse> facturas(String busqueda, LocalDate desde, LocalDate hasta);
    FacturaResponse factura(Integer idFactura);
    FacturaResponse enviarFactura(Integer idFactura, Usuario contador);
}
