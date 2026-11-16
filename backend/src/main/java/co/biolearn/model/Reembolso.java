package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de reembolso de un pago aprobado (Pagos, Fase 2). El estudiante elige uno o varios cursos del pago.
 * Estados: solicitado → aprobado (falta la devolución manual) → devuelto | devuelto (devolución automática al mismo medio);
 * solicitado → rechazado | cancelado (el curso vuelve a su estado anterior).
 * Medio de la devolución: mismo_medio (tarjeta y Nequi), cuenta_bancaria o nequi (PSE y transferencia: datos que da el estudiante).
 */
@Entity
@Table(name = "reembolso")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reembolso")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    private Pago pago;

    /** Usuario del estudiante que solicitó el reembolso. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_solicitante", nullable = false)
    private Usuario solicitante;

    /** Contador que aprobó o rechazó la solicitud. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contador")
    private PerfilContador contador;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "medio_devolucion", nullable = false, length = 20)
    @Builder.Default
    private String medioDevolucion = "mismo_medio";

    @Column(name = "banco", length = 100)
    private String banco;

    @Column(name = "tipo_cuenta", length = 20)
    private String tipoCuenta;

    @Column(name = "numero_cuenta", length = 20)
    private String numeroCuenta;

    @Column(name = "titular", length = 150)
    private String titular;

    @Column(name = "documento_titular", length = 15)
    private String documentoTitular;

    @Column(name = "telefono_nequi", length = 10)
    private String telefonoNequi;

    /** Motivo del rechazo (el estudiante lo ve). */
    @Column(name = "justificacion", length = 500)
    private String justificacion;

    /** Aprobada sola al solicitarla: todos los cursos con 0 % de avance y dentro del plazo (migración 009). */
    @Column(name = "aprobacion_automatica", nullable = false)
    @Builder.Default
    private Boolean aprobacionAutomatica = false;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "solicitado";

    @Column(name = "solicitado_en", nullable = false, updatable = false)
    private LocalDateTime solicitadoEn;

    @Column(name = "resuelto_en")
    private LocalDateTime resueltoEn;

    @Column(name = "referencia_devolucion", length = 200)
    private String referenciaDevolucion;

    @Column(name = "comprobante_devolucion_url", length = 500)
    private String comprobanteDevolucionUrl;

    @Column(name = "devuelto_en")
    private LocalDateTime devueltoEn;

    @OneToMany(mappedBy = "reembolso", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<ReembolsoInscripcion> items = new ArrayList<>();

    @PrePersist
    protected void alCrear() { if (solicitadoEn == null) solicitadoEn = LocalDateTime.now(); }
}
