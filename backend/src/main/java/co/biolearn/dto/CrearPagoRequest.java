package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

/** Checkout del carrito: inscripciones pendientes de pago y el medio (tarjeta, pse, nequi o transferencia). */
@Data @NoArgsConstructor @AllArgsConstructor
public class CrearPagoRequest {

    @NotEmpty(message = "Selecciona al menos un curso del carrito")
    private List<Integer> idsInscripcion;

    @NotBlank(message = "Indica el medio de pago")
    private String medio;
}
