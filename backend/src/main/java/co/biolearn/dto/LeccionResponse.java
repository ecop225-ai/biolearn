package co.biolearn.dto;

import lombok.*;

import java.util.List;

/** Lección con sus materiales y recursos (en el listado, materiales viene vacío y solo se informan los conteos). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeccionResponse {
    private Integer id;
    private Integer idCurso;
    private String curso;
    private String nombre;
    private String descripcion;
    private String tipo;
    private String estado;
    private Integer orden;
    private String imagenPortada;
    private Integer idPrerequisito;
    private String prerequisito;
    private long totalMateriales;
    private long materialesActivos;
    private List<Material> materiales;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Material {
        private Integer id;
        private String nombre;
        private String tipo;
        private String descripcion;
        private Integer orden;
        private String estado;
        private String creadoPor;
        /** Datos propios del tipo de material (Fase 2, diseño original del prototipo). */
        private String contenido;
        private String colorFondo;
        private String guia;
        private String url;
        private String proveedor;
        private Integer duracionMin;
        private String autores;
        private String revista;
        private String doi;
        private List<RecursoItem> recursos;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RecursoItem {
        private Integer id;
        private String nombre;
        private String descripcion;
        private String url;
        private String tipoRecurso;
        private Integer duracionMin;
        private String nivelAcademico;
        /** primario (recurso principal del material) o complementario (recurso de apoyo). */
        private String tipoContenido;
        private Integer orden;
        private String estado;
    }
}
