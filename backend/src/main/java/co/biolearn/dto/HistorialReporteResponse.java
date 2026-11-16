package co.biolearn.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialReporteResponse {
    private Integer id;
    private String tipo;
    private String tipoNombre;
    private LocalDate desde;
    private LocalDate hasta;
    private String formato;
    private String motivo;
    private String generadoPor;
    private LocalDateTime generadoEn;
}
