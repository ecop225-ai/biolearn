package co.biolearn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratarCursoRequest {

    @NotNull(message = "La institución es obligatoria")
    private Integer idInstitucion;
}
