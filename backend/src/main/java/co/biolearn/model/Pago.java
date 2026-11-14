package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pago de un estudiante. Cubre una o varias inscripciones en pendiente_pago (carrito).
 * Medios: wompi_tarjeta, pse, wompi_nequi (en línea, los resuelve la pasarela) y transferencia (la confirma un contador).
 * Estados: pendiente → aprobado | rechazado | cancelado; aprobado → reembolsado (Fase 2).
 */
@Entity
@Table(name = "pago")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private PerfilEstudiante estudiante;

    /** Contador que confirmó o rechazó la transferencia (null en los pagos en línea). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contador")
    private PerfilContador contador;

    @Column(name = "medio_pago", nullable = false, length = 20)
    private String medioPago;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Fecha en que el pago quedó aprobado. */
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    /** Identificador de la transacción en la pasarela. */
    @Column(name = "referencia_externa", length = 200)
    private String referenciaExterna;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "pendiente";

    @Column(name = "motivo_rechazo", length = 500)
    private String motivoRechazo;

    @Column(name = "comprobante_url", length = 500)
    private String comprobanteUrl;

    @OneToMany(mappedBy = "pago", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private List<PagoInscripcion> items = new ArrayList<>();

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void alCrear() { creadoEn = actualizadoEn = LocalDateTime.now(); }

    @PreUpdate
    protected void alActualizar() { actualizadoEn = LocalDateTime.now(); }
}
