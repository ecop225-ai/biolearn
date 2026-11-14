package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Material de una lección (lectura, video, laboratorio, juego, documento o artículo). NOT NULL: nombre, tipo, orden y estado. */
@Entity
@Table(name = "material_contenido")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterialContenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_material")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_leccion", nullable = false)
    private Leccion leccion;

    /** Docente que creó el material. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private PerfilDocente docente;

    @Column(name = "nombre", nullable = false, length = 250)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /** Contenido de la lectura (HTML del editor enriquecido). Migración 003. */
    @Column(name = "contenido", columnDefinition = "MEDIUMTEXT")
    private String contenido;

    /** Color de fondo de la lectura (#RRGGBB). Migración 003. */
    @Column(name = "color_fondo", length = 7)
    private String colorFondo;

    /** Guía de observación de videos, laboratorios y juegos (HTML). Migración 003. */
    @Column(name = "guia", columnDefinition = "MEDIUMTEXT")
    private String guia;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 1;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void alCrear() { creadoEn = actualizadoEn = LocalDateTime.now(); }

    @PreUpdate
    protected void alActualizar() { actualizadoEn = LocalDateTime.now(); }
}
