package co.biolearn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/** Reemplaza la lista completa de horarios del curso (una lista vacía los elimina todos). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorariosCursoRequest {

    @NotNull(message = "Envía la lista de horarios (puede estar vacía)")
    @Valid
    @Builder.Default
    private List<HorarioRequest> horarios = new ArrayList<>();
}
