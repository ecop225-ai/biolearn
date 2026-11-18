package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Vista del Administrador: el curso con sus docentes, horarios e instituciones que lo contratan. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoResponse {

    private Integer id;
    private String nombre;
    private String descripcion;
    private String objetivos;
    private String imagenPortada;
    private String codigo;
    private String color;
    private String infoEstudiante;
    private BigDecimal precio;
    private Boolean soloInstitucional;
    private Integer creditos;
    private Integer horasEstimadas;
    private Integer mesesParaTerminar;
    private Integer mesesRepaso;
    private String estado;
    private Integer idArea;
    private String area;
    private Integer idGrado;
    private String grado;
    /** Id de usuario y nombre del docente titular (null si aún no tiene). */
    private Integer idDocenteTitular;
    private String docenteTitular;
    /** Quién lo creó: "Administrador" o "Docente". */
    private String creadoPor;
    private LocalDate fechaCreacion;
    private List<DocenteCurso> docentes;
    private List<Horario> horarios;
    private List<InstitucionContratada> instituciones;
    /** Qué le falta para poder publicarse (vacío si ya cumple o si no está en borrador). */
    private List<String> pendientesParaPublicar;
    /** Fase 2: número de lecciones y de lecciones activas. */
    private long totalLecciones;
    private long leccionesActivas;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DocenteCurso {
        private Integer idDocente;
        private String nombre;
        private String correo;
        private String rol;
        private String estado;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Horario {
        private String dia;
        private LocalTime horaInicio;
        private LocalTime horaFin;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InstitucionContratada {
        private Integer idInstitucion;
        private String nombre;
        private String estado;
        /** Fase 3: código que el colegio entrega a sus estudiantes y cuántos se inscribieron con él. */
        private String codigoInscripcion;
        private long inscritos;
        private LocalDate contratadoEn;
    }
}
