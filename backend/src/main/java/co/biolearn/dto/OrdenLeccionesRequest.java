package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

/** Nuevo orden de las lecciones: los ids de TODAS las lecciones no archivadas del curso, en el orden deseado. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenLeccionesRequest {

    @NotEmpty(message = "Envía la lista de lecciones en el nuevo orden")
    private List<Integer> lecciones;
}
