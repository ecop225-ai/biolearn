package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/** URL del comprobante subido con POST /api/archivos (PDF, PNG o JPG). */
@Data @NoArgsConstructor @AllArgsConstructor
public class ComprobanteRequest {
    @NotBlank(message = "Adjunta el comprobante de la transferencia")
    private String url;
}
