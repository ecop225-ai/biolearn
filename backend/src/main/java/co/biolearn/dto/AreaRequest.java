package co.biolearn.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Al crear: el nombre es obligatorio (se valida en el servicio). Al editar: todo es opcional. */
@Data
public class AreaRequest {

    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String nombre;

    private String descripcion;
}
