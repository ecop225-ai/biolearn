package co.biolearn.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/** Vista pública de un curso activo (catálogo): sin datos internos como instituciones o pendientes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoCatalogoResponse {

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
    private String area;
    private String grado;
    private String docenteTitular;
    private List<CursoResponse.Horario> horarios;
}
