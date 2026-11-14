package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Cursos que una institución contrata para sus estudiantes (la base para las inscripciones de la Fase 3). */
@Entity
@Table(name = "institucion_curso_contratado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitucionCursoContratado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_institucion_curso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    /** Código que el colegio entrega a sus estudiantes para inscribirse vinculados a la institución (migración 005). */
    @Column(name = "codigo_inscripcion", nullable = false, length = 8, unique = true)
    private String codigoInscripcion;

    @Column(name = "contratado_en", nullable = false, updatable = false)
    private LocalDateTime contratadoEn;

    @PrePersist
    protected void alCrear() {
        this.contratadoEn = LocalDateTime.now();
    }
}
