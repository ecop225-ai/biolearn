package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Inicio del contador: cifras del mes, pendientes y últimos movimientos. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ResumenFinancieroResponse {
    private BigDecimal ingresosMes;
    private long pagosMes;
    private BigDecimal reembolsadoMes;
    private BigDecimal netoMes;
    private long transferenciasPorConfirmar;
    private long reembolsosPorRevisar;
    private long reembolsosPorDevolver;
    private BigDecimal montoPorDevolver;
    private List<Mes> ultimosMeses;
    private List<Movimiento> movimientos;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Mes {
        /** AAAA-MM */
        private String mes;
        private BigDecimal ingresos;
        private BigDecimal reembolsos;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Movimiento {
        private LocalDateTime fecha;
        /** pago | reembolso */
        private String tipo;
        private Integer id;
        private String descripcion;
        private String estudiante;
        private BigDecimal monto;
        private String estado;
    }
}
