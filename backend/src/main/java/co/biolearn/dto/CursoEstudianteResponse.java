package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Un curso visto por el estudiante en el catálogo o en su detalle, con su inscripción si la tiene. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoEstudianteResponse {
    private Integer id;
    private String nombre;
    private String codigo;
    private String color;
    private String imagenPortada;
    private String descripcion;
    private String objetivos;
    private String infoEstudiante;
    private String area;
    private String grado;
    private BigDecimal precio;
    private boolean gratuito;
    private Integer creditos;
    private Integer horasEstimadas;
    private String docenteTitular;
    private long totalLecciones;
    /** true si la institución del estudiante contrató el curso. */
    private boolean deMiColegio;
    /** Solo para colegios: el estudiante se inscribe con el código que le dio su colegio. */
    private boolean soloInstitucional;
    private Integer idInscripcion;
    private String estadoInscripcion;
    private BigDecimal porcentaje;
    /** Solo en el detalle: los títulos de las lecciones activas, en orden. */
    private List<String> lecciones;
    /** Solo al buscar por código: la institución que contrató el curso. */
    private String institucionCodigo;
}
