package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Cursos del estudiante pendientes de pago que aún no están en un pago en proceso. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CarritoResponse {
    private List<PagoResponse.Item> items;
    private BigDecimal total;
    /** Cursos pendientes que ya tienen un pago en proceso (no se pueden volver a pagar). */
    private List<PagoResponse.Item> enProceso;
}
