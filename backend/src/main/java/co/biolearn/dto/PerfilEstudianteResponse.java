package co.biolearn.dto;

import lombok.*;

/** Datos básicos del estudiante que inició sesión: su institución, grado y área. */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PerfilEstudianteResponse {
    private Integer idEstudiante;
    private String nombre;
    private String correo;
    private Integer idInstitucion;
    private String institucion;
    private String grado;
    private String area;
}
