package co.biolearn.dto;

import lombok.*;

/** Archivo subido: la URL para usarla en un recurso o en una portada. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ArchivoResponse {
    private String url;
    private String nombreOriginal;
    private String tipo;
    private long tamanoBytes;
}
