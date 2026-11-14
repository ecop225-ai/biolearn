package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Un pago con sus cursos; para el contador incluye el estudiante y quién lo resolvió. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoResponse {
    private Integer id;
    private String medio;
    private String medioNombre;
    private Boolean enLinea;
    private String estado;
    private BigDecimal monto;
    private LocalDateTime creadoEn;
    private LocalDateTime fechaPago;
    private String referencia;
    private String motivoRechazo;
    private String comprobanteUrl;
    private List<Item> items;
    private Integer idFactura;
    private String numeroFactura;
    private Integer idEstudiante;
    private String estudiante;
    private String correo;
    private String resueltoPor;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private Integer idInscripcion;
        private Integer idCurso;
        private String curso;
        private String codigoCurso;
        private BigDecimal monto;
    }
}
