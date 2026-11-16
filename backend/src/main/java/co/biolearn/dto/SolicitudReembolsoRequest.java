package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Solicitud de reembolso: el pago, los cursos elegidos y el motivo. Los datos de la devolución solo se piden
 * cuando el pago fue por PSE o transferencia (medioDevolucion: cuenta_bancaria o nequi); con tarjeta o Nequi
 * se devuelve al mismo medio.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SolicitudReembolsoRequest {
    @NotNull(message = "Indica el pago")
    private Integer idPago;

    @NotEmpty(message = "Selecciona al menos un curso para reembolsar")
    private List<Integer> idsInscripcion;

    @NotBlank(message = "Escribe el motivo del reembolso")
    @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres")
    private String motivo;

    /** mismo_medio, cuenta_bancaria o nequi. */
    private String medioDevolucion;
    private String banco;
    /** ahorros o corriente. */
    private String tipoCuenta;
    private String numeroCuenta;
    private String titular;
    private String documentoTitular;
    private String telefonoNequi;
}
