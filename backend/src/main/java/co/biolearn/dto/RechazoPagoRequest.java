package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Motivo con que el contador rechaza una transferencia; el estudiante lo ve en «Mis pagos». */
@Data @NoArgsConstructor @AllArgsConstructor
public class RechazoPagoRequest {
    @NotBlank(message = "Escribe el motivo del rechazo")
    @Size(max = 500, message = "El motivo admite máximo 500 caracteres")
    private String motivo;
}
