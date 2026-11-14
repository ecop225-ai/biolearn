package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioRequest {

    @NotBlank(message = "El día es obligatorio")
    private String dia;

    @NotNull(message = "La hora de inicio es obligatoria (formato HH:mm)")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria (formato HH:mm)")
    private LocalTime horaFin;
}
