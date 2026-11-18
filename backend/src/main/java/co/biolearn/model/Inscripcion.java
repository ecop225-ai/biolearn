package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Inscripción de un estudiante en un curso. Estados: pendiente_pago (curso con precio, sin acceso),
 * activa, completada, retirada y suspendida. Un estudiante tiene una sola inscripción por curso (uq_inscripcion).
 */
@Entity
@Table(name = "inscripcion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private PerfilEstudiante estudiante;

    /** Contratación cuyo código usó el estudiante (vincula la inscripción a su institución). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion_curso")
    private InstitucionCursoContratado contratacion;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "activa";

    @Column(name = "fecha_inscripcion", nullable = false, updatable = false)
    private LocalDateTime fechaInscripcion;

    @Column(name = "fecha_completacion")
    private LocalDateTime fechaCompletacion;

    /** Acceso por tiempo (migración 011): inicio y fin del plazo para terminar el curso. */
    @Column(name = "acceso_desde")
    private LocalDateTime accesoDesde;

    @Column(name = "acceso_hasta")
    private LocalDateTime accesoHasta;

    /** Fin del repaso (solo lectura) de un curso completado. */
    @Column(name = "repaso_hasta")
    private LocalDateTime repasoHasta;

    /** Avisos de 15 días ya enviados (una sola vez por plazo). */
    @Column(name = "aviso_vencimiento_en")
    private LocalDateTime avisoVencimientoEn;

    @Column(name = "aviso_repaso_en")
    private LocalDateTime avisoRepasoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void alCrear() { fechaInscripcion = actualizadoEn = LocalDateTime.now(); }

    @PreUpdate
    protected void alActualizar() { actualizadoEn = LocalDateTime.now(); }
}
