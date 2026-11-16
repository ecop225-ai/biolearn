package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Nueva cuenta bancaria (y Nequi opcional) donde el estudiante transfiere (Pagos, Fase 3). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatosRecaudoRequest {
    @NotBlank(message = "Escribe el banco")
    @Size(max = 80, message = "El banco admite máximo 80 caracteres")
    private String banco;

    @NotBlank(message = "Elige el tipo de cuenta")
    @Pattern(regexp = "^(ahorros|corriente)$", message = "El tipo de cuenta debe ser ahorros o corriente")
    private String tipoCuenta;

    @NotBlank(message = "Escribe el número de cuenta")
    @Pattern(regexp = "^\\d{6,20}$", message = "El número de cuenta debe tener entre 6 y 20 dígitos")
    private String numeroCuenta;

    @NotBlank(message = "Escribe el titular de la cuenta")
    @Size(max = 150, message = "El titular admite máximo 150 caracteres")
    private String titular;

    @NotBlank(message = "Escribe el NIT o documento del titular")
    @Pattern(regexp = "^\\d{5,15}(-\\d)?$", message = "El NIT o documento del titular tiene de 5 a 15 dígitos")
    private String documentoTitular;

    /** Opcional: Nequi para recibir pagos (10 dígitos, empieza por 3). */
    @Pattern(regexp = "^$|^3\\d{9}$", message = "El Nequi debe tener 10 dígitos y empezar por 3")
    private String telefonoNequi;

    @Size(max = 500, message = "Las instrucciones admiten máximo 500 caracteres")
    private String instrucciones;
}
