package co.biolearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta simple de confirmación, para endpoints que no devuelven un recurso (ej. "correo enviado"). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MensajeResponse {
    private String mensaje;
}
