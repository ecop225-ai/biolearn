package co.biolearn.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/** Crear o editar un material. Al crear, nombre y tipo son obligatorios (se valida en el servicio). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterialRequest {

    @Size(max = 250, message = "El nombre no puede superar 250 caracteres")
    private String nombre;

    /** lectura, video, laboratorio, juego, documento o articulo. */
    private String tipo;

    private String descripcion;

    /** Lectura: contenido HTML del editor enriquecido y color de fondo (#RRGGBB). */
    private String contenido;

    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "El color de fondo debe tener el formato #RRGGBB")
    private String colorFondo;

    /** Video, laboratorio y juego: guía de observación (HTML). */
    private String guia;

    /** Video (YouTube), laboratorio o juego (externo), documento (archivo subido) o artículo. */
    @Size(max = 1000, message = "La URL no puede superar 1000 caracteres")
    private String url;

    /** Laboratorio o juego: proveedor externo (Labster, PhET, Kahoot...). */
    @Size(max = 200, message = "El proveedor no puede superar 200 caracteres")
    private String proveedor;

    @Min(value = 1, message = "La duración debe ser mayor que 0 minutos")
    @Max(value = 1000, message = "La duración no puede superar 1000 minutos")
    private Integer duracionMin;

    /** Artículo científico. */
    @Size(max = 500, message = "Los autores no pueden superar 500 caracteres")
    private String autores;

    @Size(max = 250, message = "La revista no puede superar 250 caracteres")
    private String revista;

    @Size(max = 100, message = "El DOI no puede superar 100 caracteres")
    private String doi;
}
