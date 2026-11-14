package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Progreso de un estudiante en un material (migraciones 005 y 006). Un registro por inscripción y material:
 * se crea al abrirlo (abierto_en) y se completa cuando el estudiante cumple el criterio de su tipo (visto_en).
 */
@Entity
@Table(name = "progreso_material")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProgresoMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_progreso_material")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material", nullable = false)
    private MaterialContenido material;

    @Column(name = "abierto_en", nullable = false)
    private LocalDateTime abiertoEn;

    /** Vacío mientras el estudiante no cumpla el criterio del tipo de material. */
    @Column(name = "visto_en")
    private LocalDateTime vistoEn;

    @PrePersist
    protected void alCrear() { if (abiertoEn == null) abiertoEn = LocalDateTime.now(); }
}
