package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Datos bibliográficos de un material de tipo «articulo». NOT NULL: titulo y autores. */
@Entity
@Table(name = "articulo_cientifico")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ArticuloCientifico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_articulo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material", nullable = false)
    private MaterialContenido material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private PerfilDocente docente;

    @Column(name = "titulo", nullable = false, length = 300)
    private String titulo;

    @Column(name = "autores", nullable = false, length = 500)
    private String autores;

    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "revista", length = 250)
    private String revista;

    @Column(name = "doi", length = 100)
    private String doi;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    protected void alCrear() { creadoEn = LocalDateTime.now(); }
}
