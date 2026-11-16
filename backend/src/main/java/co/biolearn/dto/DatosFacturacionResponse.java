package co.biolearn.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatosFacturacionResponse {
    private Integer id;
    private String razonSocial;
    private String nit;
    private String direccion;
    private String ciudad;
    private String correo;
    private String telefono;
    private LocalDateTime actualizadoEn;
    /** Nombre de quien guardó la versión; null en el registro inicial. */
    private String actualizadoPor;
    private boolean vigente;
}
