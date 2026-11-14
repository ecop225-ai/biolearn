package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearCursoRequest {

    @NotBlank(message = "El nombre del curso es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @NotNull(message = "El área académica es obligatoria")
    private Integer idArea;

    @NotNull(message = "El grado es obligatorio")
    private Integer idGrado;

    private String descripcion;

    private String objetivos;

    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    private BigDecimal precio;

    @Min(value = 1, message = "Los créditos deben ser mayores que 0")
    @Max(value = 100, message = "Los créditos no pueden superar 100")
    private Integer creditos;

    @Min(value = 1, message = "Las horas estimadas deben ser mayores que 0")
    @Max(value = 10000, message = "Las horas estimadas no pueden superar 10000")
    private Integer horasEstimadas;

    @Size(max = 500, message = "La URL de la portada no puede superar 500 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "La portada debe ser una URL que empiece por http:// o https://")
    private String imagenPortada;

    /** Id de USUARIO del docente titular (el mismo que muestra la Gestión de usuarios). Opcional al crear. */
    private Integer idDocenteTitular;
}
