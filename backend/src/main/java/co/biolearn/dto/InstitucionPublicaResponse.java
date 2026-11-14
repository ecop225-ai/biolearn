package co.biolearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo mínimo que se expone públicamente de una institución al validar su código en el registro. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitucionPublicaResponse {
    private String nombre;
}
