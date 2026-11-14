package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Avance de un estudiante en una lección: pendiente, en_progreso o completada, con su porcentaje. */
@Entity
@Table(name = "progreso_leccion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProgresoLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_progreso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_leccion", nullable = false)
    private Leccion leccion;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "pendiente";

    @Column(name = "porcentaje", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal porcentaje = BigDecimal.ZERO;

    @Column(name = "iniciado_en")
    private LocalDateTime iniciadoEn;

    @Column(name = "completado_en")
    private LocalDateTime completadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    protected void alGuardar() { actualizadoEn = LocalDateTime.now(); }
}
