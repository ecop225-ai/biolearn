package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/** Parámetros de un reporte financiero. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReporteRequest {
    @NotBlank(message = "Elige el tipo de reporte")
    @Pattern(regexp = "^(ingresos|facturas|reembolsos|general)$", message = "El tipo de reporte debe ser ingresos, facturas, reembolsos o general")
    private String tipo;

    @NotNull(message = "Elige la fecha inicial del período")
    private LocalDate desde;

    @NotNull(message = "Elige la fecha final del período")
    private LocalDate hasta;

    @NotBlank(message = "Escribe el motivo del reporte")
    @Size(min = 5, max = 300, message = "El motivo debe tener entre 5 y 300 caracteres")
    private String motivo;
}
