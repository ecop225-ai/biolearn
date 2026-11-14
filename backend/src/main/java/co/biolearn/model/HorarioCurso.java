package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "horario_curso")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    /** lunes, martes, miercoles, jueves, viernes, sabado o domingo (sin tildes, como el ENUM de la base). */
    @Column(name = "dia_semana", nullable = false, length = 10)
    private String diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;
}
