package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Qué cursos de un pago se pueden reembolsar, hasta cuándo y si hay que pedir los datos de la devolución. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OpcionesReembolsoResponse {
    private Integer idPago;
    private String medioNombre;
    private LocalDateTime fechaPago;
    private LocalDateTime plazoHasta;
    private Boolean enPlazo;
    /** true: tarjeta o Nequi (se devuelve al mismo medio); false: PSE o transferencia (se piden cuenta bancaria o Nequi). */
    private Boolean devolucionAlMismoMedio;
    private Integer diasPlazo;
    private BigDecimal avanceMaximo;
    private List<Curso> cursos;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Curso {
        private Integer idInscripcion;
        private String curso;
        private String codigoCurso;
        private BigDecimal monto;
        private BigDecimal avance;
        private String estadoInscripcion;
        private Boolean reembolsable;
        /** Por qué no se puede reembolsar (null si se puede). */
        private String motivo;
    }
}
