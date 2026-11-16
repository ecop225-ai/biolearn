package co.biolearn.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatosRecaudoResponse {
    private Integer id;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private String titular;
    private String documentoTitular;
    private String telefonoNequi;
    private String instrucciones;
    private LocalDateTime actualizadoEn;
    private String actualizadoPor;
    private boolean vigente;
}
