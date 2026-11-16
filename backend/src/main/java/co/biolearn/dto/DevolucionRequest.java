package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** El contador registra la devolución hecha por fuera de la pasarela (cuenta bancaria o Nequi). */
@Data @NoArgsConstructor @AllArgsConstructor
public class DevolucionRequest {
    @NotBlank(message = "Escribe la referencia o el número de la transacción de la devolución")
    @Size(max = 200, message = "La referencia admite máximo 200 caracteres")
    private String referencia;

    /** Opcional: comprobante subido a la plataforma (/api/archivos/…). */
    private String comprobanteUrl;
}
