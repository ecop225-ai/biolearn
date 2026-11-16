package co.biolearn.dto;

import lombok.*;

/** Datos vigentes de facturación y de recaudo, y si quien consulta puede editarlos. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionFinancieraResponse {
    private DatosFacturacionResponse facturacion;
    private DatosRecaudoResponse recaudo;
    /** El Administrador y el contador administrador financiero editan; el auxiliar y el auditor consultan. */
    private boolean puedeEditar;
}
