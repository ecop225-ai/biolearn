package co.biolearn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgregarDocenteCursoRequest {

    /** Id de USUARIO del docente. */
    @NotNull(message = "El docente es obligatorio")
    private Integer idDocente;

    /** colaborador o invitado (el titular se asigna editando el curso). */
    @NotBlank(message = "El rol es obligatorio: colaborador o invitado")
    private String rol;
}
