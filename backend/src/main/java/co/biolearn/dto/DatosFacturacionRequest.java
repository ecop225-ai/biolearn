package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Nuevos datos del emisor de las facturas y notas crédito (Pagos, Fase 3). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatosFacturacionRequest {
    @NotBlank(message = "Escribe la razón social")
    @Size(max = 200, message = "La razón social admite máximo 200 caracteres")
    private String razonSocial;

    @NotBlank(message = "Escribe el NIT")
    @Pattern(regexp = "^\\d{6,12}(-\\d)?$", message = "El NIT tiene de 6 a 12 dígitos y, si quieres, el dígito de verificación (901000000-0)")
    private String nit;

    @Size(max = 255, message = "La dirección admite máximo 255 caracteres")
    private String direccion;

    @Size(max = 100, message = "La ciudad admite máximo 100 caracteres")
    private String ciudad;

    @Email(message = "El correo de facturación no es válido")
    @Size(max = 150, message = "El correo admite máximo 150 caracteres")
    private String correo;

    @Pattern(regexp = "^$|^[0-9 +()-]{7,20}$", message = "El teléfono tiene de 7 a 20 caracteres (números, espacios, +, paréntesis o guiones)")
    private String telefono;
}
