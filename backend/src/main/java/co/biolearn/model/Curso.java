package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Curso de la plataforma. Lo crea el Administrador (id_admin_creador) o, desde la Fase 2, un docente
 * (en ese caso queda en borrador con él como titular). Solo el Administrador lo publica.
 * Estados: borrador → activo ⇄ cerrado; cualquiera → archivado; archivado → borrador.
 * No existe borrado físico: "eliminar" un curso es archivarlo, para conservar su historial.
 */
@Entity
@Table(name = "curso")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grado", nullable = false)
    private Grado grado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area", nullable = false)
    private AreaAcademica area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_admin_creador")
    private PerfilAdmin adminCreador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente_titular")
    private PerfilDocente docenteTitular;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /** Código visible del curso (por ejemplo, GEN-101); único si se define. Migración 003. */
    @Column(name = "codigo", length = 20)
    private String codigo;

    @Column(name = "imagen_portada", length = 500)
    private String imagenPortada;

    /** Color representativo (#RRGGBB) para la tarjeta del curso. Migración 003. */
    @Column(name = "color", length = 7)
    private String color;

    /** Información para el estudiante antes de inscribirse (HTML del editor). Migración 003. */
    @Column(name = "info_estudiante", columnDefinition = "MEDIUMTEXT")
    private String infoEstudiante;

    /** TEXT NOT NULL en la base: un borrador sin descripción guarda "" (para publicar se exige contenido). */
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String descripcion = "";

    /** TEXT NOT NULL en la base. */
    @Column(name = "objetivos", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String objetivos = "";

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal precio = BigDecimal.ZERO;

    /** Solo para colegios: no aparece en el catálogo abierto; se inscribe con el código del colegio (migración 009). */
    @Column(name = "solo_institucional", nullable = false)
    @Builder.Default
    private Boolean soloInstitucional = false;

    /** TINYINT UNSIGNED NOT NULL DEFAULT 0 en la base: 0 significa "sin créditos definidos". */
    @Column(name = "creditos", nullable = false)
    @Builder.Default
    private Integer creditos = 0;

    @Column(name = "horas_estimadas")
    private Integer horasEstimadas;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "borrador";

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void alCrear() {
        LocalDateTime ahora = LocalDateTime.now();
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    @PreUpdate
    protected void alActualizar() {
        this.actualizadoEn = LocalDateTime.now();
    }
}
