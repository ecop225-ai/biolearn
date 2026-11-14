package co.biolearn.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private List<String> detalles;

    /** Código legible por máquina para que el frontend reaccione a casos puntuales
     *  (ej. CUENTA_NO_VERIFICADA → ofrecer "Activar mi cuenta"). Se omite del JSON si es null. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String codigo;
}
