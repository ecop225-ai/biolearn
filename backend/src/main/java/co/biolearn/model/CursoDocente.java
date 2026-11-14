package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Docentes asignados a un curso: el titular (sincronizado con curso.id_docente_titular), colaboradores e invitados.
 *  Retirar a un docente no borra la fila: la deja inactiva, para conservar el historial. */
@Entity
@Table(name = "curso_docente")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CursoDocente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso_docente")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private PerfilDocente docente;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo";

    @Column(name = "asignado_en", nullable = false, updatable = false)
    private LocalDateTime asignadoEn;

    @PrePersist
    protected void alCrear() {
        this.asignadoEn = LocalDateTime.now();
    }
}
