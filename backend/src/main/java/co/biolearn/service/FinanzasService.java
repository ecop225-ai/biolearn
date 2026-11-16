package co.biolearn.service;

import co.biolearn.dto.*;
import co.biolearn.model.Usuario;

import java.util.List;

/** Pagos, Fase 3: datos de facturación y de recaudo editables, e inicio del contador. */
public interface FinanzasService {

    ConfiguracionFinancieraResponse configuracion(Usuario usuario);

    DatosFacturacionResponse actualizarFacturacion(DatosFacturacionRequest request, Usuario usuario);

    DatosRecaudoResponse actualizarRecaudo(DatosRecaudoRequest request, Usuario usuario);

    List<DatosFacturacionResponse> historialFacturacion();

    List<DatosRecaudoResponse> historialRecaudo();

    /** Cuenta y Nequi vigentes para que el estudiante transfiera. */
    DatosRecaudoResponse recaudoVigente();

    ResumenFinancieroResponse resumen();
}
