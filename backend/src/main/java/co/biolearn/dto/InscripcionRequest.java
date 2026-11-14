package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Inscribirse en un curso; el código de inscripción de la institución es opcional. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InscripcionRequest {

    @NotNull(message = "Indica el curso")
    private Integer idCurso;

    @Size(max = 8, message = "El código de inscripción tiene 8 caracteres")
    private String codigo;
}
