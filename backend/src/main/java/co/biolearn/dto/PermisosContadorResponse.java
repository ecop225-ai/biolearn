package co.biolearn.dto;

import lombok.*;

/** Qué puede hacer el contador que inició sesión: el auditor solo consulta. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PermisosContadorResponse {
    private String rolContador;
    private Boolean puedeResolver;
}
