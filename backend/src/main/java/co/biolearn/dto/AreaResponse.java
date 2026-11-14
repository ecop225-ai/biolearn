package co.biolearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String estado;
    /** Perfiles de estudiante que tienen asignado este registro. */
    private long estudiantesAsociados;
}
