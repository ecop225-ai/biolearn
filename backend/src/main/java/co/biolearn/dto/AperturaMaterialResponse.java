package co.biolearn.dto;

import lombok.*;

/** Resultado de abrir un material: cuánto tiempo mínimo debe revisarse y cuánto lleva el estudiante. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AperturaMaterialResponse {
    private Integer idMaterial;
    private String tipo;
    /** Segundos que deben pasar desde la apertura para que el servidor acepte el «visto». */
    private long segundosMinimos;
    /** Segundos transcurridos desde la primera apertura. */
    private long segundosTranscurridos;
    private boolean visto;
}
