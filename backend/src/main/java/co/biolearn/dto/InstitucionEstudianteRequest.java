package co.biolearn.dto;

import lombok.*;

/** El Administrador cambia la institución de un estudiante; idInstitucion null lo deja como independiente. */
@Data @NoArgsConstructor @AllArgsConstructor
public class InstitucionEstudianteRequest {
    private Integer idInstitucion;
}
