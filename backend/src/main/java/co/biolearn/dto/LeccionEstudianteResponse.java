package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Una lección vista por el estudiante: estado de su progreso, si está bloqueada y, en el detalle, sus materiales. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeccionEstudianteResponse {
    private Integer id;
    private Integer idCurso;
    private String curso;
    private String nombre;
    private String descripcion;
    private String tipo;
    private Integer orden;
    private String imagenPortada;
    private Integer idPrerequisito;
    private String prerequisito;
    private boolean bloqueada;
    /** pendiente, en_progreso o completada. */
    private String estadoProgreso;
    private BigDecimal porcentaje;
    private long materialesVistos;
    private long totalMateriales;
    /** Porcentaje del curso completo (para la barra de avance). */
    private BigDecimal porcentajeCurso;
    private String estadoInscripcion;
    /** Curso completado en repaso (solo lectura) y fecha en que termina. */
    private boolean modoRepaso;
    private LocalDateTime repasoHasta;
    /** Solo en el detalle: materiales y recursos activos, con «visto». */
    private List<MaterialEstudiante> materiales;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MaterialEstudiante {
        private boolean visto;
        /** Tiempo mínimo de revisión de este material (marcado automático, migración 006). */
        private long segundosMinimos;
        private LeccionResponse.Material material;
    }
}
