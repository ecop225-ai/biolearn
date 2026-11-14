package co.biolearn.dto;

import lombok.*;

/**
 * Datos de la pasarela de pruebas, como en el sandbox de Wompi:
 * tarjeta → numeroTarjeta; nequi → telefono; pse → banco ("1" aprueba, "2" rechaza).
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoPasarelaRequest {
    private String numeroTarjeta;
    private String telefono;
    private String banco;
}
