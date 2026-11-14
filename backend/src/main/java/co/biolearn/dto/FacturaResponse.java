package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Factura con los datos del emisor guardados al emitirla y los cursos pagados. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class FacturaResponse {
    private Integer id;
    private String numero;
    private LocalDateTime fechaEmision;
    private String estado;
    private String razonSocial;
    private String nit;
    private String direccionEmisor;
    private Integer idEstudiante;
    private String estudiante;
    private String correo;
    private String documento;
    private String descripcion;
    private List<PagoResponse.Item> items;
    private BigDecimal montoTotal;
    private BigDecimal montoPagado;
    private Integer idPago;
    private String medioNombre;
    private String referencia;
    private Boolean enviadaPorCorreo;
    private LocalDateTime enviadaEn;
}
