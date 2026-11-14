package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Notificación para un usuario; entidadId apunta al registro del tipo (p. ej. el pago). */
@Entity
@Table(name = "notificacion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_emisor")
    private Usuario emisor;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "entidad_id")
    private Integer entidadId;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    @Column(name = "estado_notificacion", nullable = false, length = 20)
    @Builder.Default
    private String estadoNotificacion = "pendiente";

    @Column(name = "leida", nullable = false)
    @Builder.Default
    private Boolean leida = false;

    @Column(name = "leida_en")
    private LocalDateTime leidaEn;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn;

    @PrePersist
    protected void alCrear() { creadaEn = LocalDateTime.now(); }
}
