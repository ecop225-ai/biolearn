package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Recurso de un material: una URL (video de YouTube en formato para embeber, archivo subido o enlace). NOT NULL: nombre, url, tipo_recurso, orden y estado. */
@Entity
@Table(name = "recurso")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material", nullable = false)
    private MaterialContenido material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private PerfilDocente docente;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private String tipoRecurso;

    @Column(name = "duracion_min")
    private Integer duracionMin;

    @Column(name = "nivel_academico", length = 20)
    private String nivelAcademico;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 1;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void alCrear() { creadoEn = LocalDateTime.now(); }
}
