package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Código de afiliación que el colegio entrega a sus estudiantes («Unirme a mi colegio» en Mi perfil). */
@Data @NoArgsConstructor @AllArgsConstructor
public class UnirseInstitucionRequest {
    @NotBlank(message = "Escribe el código de afiliación de tu colegio")
    @Size(min = 8, max = 8, message = "El código de afiliación tiene 8 caracteres")
    private String codigoAfiliacion;
}
