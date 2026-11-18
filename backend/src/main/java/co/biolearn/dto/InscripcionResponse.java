package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Una inscripción del estudiante (o de un inscrito, en la vista del docente y del Administrador). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InscripcionResponse {
    private Integer id;
    private Integer idCurso;
    private String curso;
    private String codigoCurso;
    private String color;
    private String imagenPortada;
    private BigDecimal precio;
    private String estado;
    private LocalDateTime fechaInscripcion;
    private LocalDateTime fechaCompletacion;
    /** Institución vinculada por el código de inscripción, si se usó. */
    private String institucion;
    private BigDecimal porcentaje;
    /** Acceso por tiempo: plazo para terminar, fin del repaso y días que faltan para la fecha que aplica. */
    private LocalDateTime accesoDesde;
    private LocalDateTime accesoHasta;
    private LocalDateTime repasoHasta;
    private Long diasRestantes;
    private boolean modoRepaso;
    /** Pendiente de pago por una renovación (ya tuvo un plazo antes). */
    private boolean renovacion;
    private Integer mesesParaTerminar;
    private Integer mesesRepaso;
    private long leccionesCompletadas;
    private long totalLecciones;
    /** Vista del docente y del Administrador. */
    private Integer idEstudiante;
    private String estudiante;
    private String correo;
}
