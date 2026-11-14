package co.biolearn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** El código de afiliación NO se recibe: lo genera el backend (8 caracteres, único). */
@Data
public class CrearInstitucionRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre admite máximo 200 caracteres")
    private String nombre;

    @NotBlank(message = "El NIT es obligatorio")
    @Pattern(regexp = "^[0-9.\\-]{5,20}$", message = "El NIT solo admite números, puntos y guiones (5 a 20 caracteres)")
    private String nit;

    @Size(max = 200, message = "El nombre del rector admite máximo 200 caracteres")
    private String rector;

    @Size(max = 300, message = "La dirección admite máximo 300 caracteres")
    private String direccion;

    @Size(max = 20, message = "El teléfono admite máximo 20 caracteres")
    private String telefono;

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo admite máximo 150 caracteres")
    private String correo;

    @NotNull(message = "Los cupos contratados son obligatorios")
    @Min(value = 1, message = "Debe contratar al menos 1 cupo")
    @Max(value = 65535, message = "Los cupos admiten máximo 65535")
    private Integer cuposContratados;
}
