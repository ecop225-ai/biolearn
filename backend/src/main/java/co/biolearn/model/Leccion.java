package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lección de un curso. Estados: borrador → activa ⇄ inactiva; cualquiera → archivada; archivada → borrador.
 * NOT NULL en la base: nombre, orden (único por curso), tipo y estado.
 */
@Entity
@Table(name = "leccion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Leccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_leccion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    /** Lección que debe completarse antes (del mismo curso y anterior en el orden). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prerequisito")
    private Leccion prerequisito;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "imagen_portada", length = 500)
    private String imagenPortada;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "tipo", nullable = false, length = 20)
    @Builder.Default
    private String tipo = "teoria";

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "borrador";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void alCrear() { creadoEn = actualizadoEn = LocalDateTime.now(); }

    @PreUpdate
    protected void alActualizar() { actualizadoEn = LocalDateTime.now(); }
}
