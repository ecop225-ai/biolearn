package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Nota crédito con los datos del emisor guardados al emitirla, la factura que corrige y los cursos reembolsados. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotaCreditoResponse {
    private Integer id;
    private String numero;
    private LocalDateTime fechaEmision;
    private String razonSocial;
    private String nit;
    private String direccionEmisor;
    private Integer idFactura;
    private String numeroFactura;
    private LocalDateTime fechaFactura;
    private Integer idEstudiante;
    private String estudiante;
    private String correo;
    private String documento;
    private String motivo;
    private List<ReembolsoResponse.Item> items;
    private BigDecimal montoTotal;
    private Integer idReembolso;
    private String medioDevolucionNombre;
    private Boolean enviadaPorCorreo;
    private LocalDateTime enviadaEn;
}
