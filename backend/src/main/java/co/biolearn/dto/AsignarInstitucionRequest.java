package co.biolearn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Asigna o cambia la institución que representa un usuario con rol supervisor (representante institucional). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsignarInstitucionRequest {

    @NotNull(message = "La institución es obligatoria")
    private Integer idInstitucion;

    /** Opcional: si se envía, actualiza también el cargo del representante. */
    @Size(max = 150, message = "El cargo no puede superar 150 caracteres")
    private String cargo;
}
