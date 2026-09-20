package co.biolearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime fecha;
    private int estadoHttp;
    private String mensaje;
    /** Lista de errores de validación campo por campo (puede ser null si no aplica). */
    private List<String> detalles;
}
