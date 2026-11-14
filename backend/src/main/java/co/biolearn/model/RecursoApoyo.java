package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Vincula los recursos y artículos a un material, con su tipo de contenido (tabla recurso_apoyo):
 * primario = recurso principal del material (el video, el laboratorio, el documento…);
 * complementario = recursos de apoyo adicionales; referencia = artículo científico.
 * NOT NULL: id_material, id_usuario, tipo_recurso, tipo_contenido, titulo, orden y estado.
 */
@Entity
@Table(name = "recurso_apoyo")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RecursoApoyo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso_apoyo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_material", nullable = false)
    private MaterialContenido material;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_recurso")
    private Recurso recurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_articulo")
    private ArticuloCientifico articulo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** documento, video, enlace, imagen, interactivo u otro. */
    @Column(name = "tipo_recurso", nullable = false, length = 20)
    private String tipoRecurso;

    /** primario, complementario, referencia o evaluativo. */
    @Column(name = "tipo_contenido", nullable = false, length = 20)
    @Builder.Default
    private String tipoContenido = "complementario";

    @Column(name = "titulo", nullable = false, length = 250)
    private String titulo;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "orden", nullable = false)
    @Builder.Default
    private Integer orden = 1;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "activo";
}
