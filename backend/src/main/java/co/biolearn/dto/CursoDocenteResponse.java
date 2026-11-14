package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Un curso visto por el docente: sus datos, su rol en el curso y el avance del contenido. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoDocenteResponse {
    private Integer id;
    private String nombre;
    private String descripcion;
    private String objetivos;
    private String imagenPortada;
    private String codigo;
    private String color;
    private String infoEstudiante;
    private BigDecimal precio;
    private Integer creditos;
    private Integer horasEstimadas;
    private String estado;
    private Integer idArea;
    private String area;
    private Integer idGrado;
    private String grado;
    private String docenteTitular;
    /** titular, colaborador o invitado. */
    private String miRol;
    /** true si el docente puede editar el contenido (titular o colaborador y curso no archivado). */
    private boolean puedeEditarContenido;
    private long totalLecciones;
    private long leccionesActivas;
    private List<String> pendientesParaPublicar;
}
