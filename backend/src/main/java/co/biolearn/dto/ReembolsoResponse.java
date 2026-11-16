package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Solicitud de reembolso con sus cursos, la devolución y la nota crédito. El número de cuenta va enmascarado
 *  para el estudiante y el auditor; completo solo para el contador que hace la devolución. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReembolsoResponse {
    private Integer id;
    private Integer idPago;
    private String medioPago;
    private String medioPagoNombre;
    private String referenciaPago;
    private LocalDateTime fechaPago;
    private String estado;
    private String motivo;
    private BigDecimal monto;
    private List<Item> items;
    private String medioDevolucion;
    private String medioDevolucionNombre;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private String titular;
    private String documentoTitular;
    private String telefonoNequi;
    private LocalDateTime solicitadoEn;
    private LocalDateTime resueltoEn;
    private String justificacion;
    private String referenciaDevolucion;
    private String comprobanteDevolucionUrl;
    private LocalDateTime devueltoEn;
    private Integer idFactura;
    private String numeroFactura;
    private Integer idNotaCredito;
    private String numeroNotaCredito;
    private Integer idEstudiante;
    private String estudiante;
    private String correo;
    private String resueltoPor;
    private Boolean aprobacionAutomatica;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private Integer idInscripcion;
        private Integer idCurso;
        private String curso;
        private String codigoCurso;
        private BigDecimal monto;
        private BigDecimal avance;
    }
}
