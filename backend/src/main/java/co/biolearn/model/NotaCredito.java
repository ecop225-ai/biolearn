package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nota crédito de un reembolso aprobado (Pagos, Fase 2): corrige la factura del pago sin anularla.
 * Una por reembolso, con una línea por curso; número BIO-NC-AAAA-000001 y los datos de facturación vigentes.
 */
@Entity
@Table(name = "nota_credito")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_credito")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reembolso", nullable = false)
    private Reembolso reembolso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura", nullable = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private PerfilEstudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_datos_facturacion")
    private DatosFacturacion datosFacturacion;

    @Column(name = "numero_nota", nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "enviada_por_correo", nullable = false)
    @Builder.Default
    private Boolean enviadaPorCorreo = false;

    @Column(name = "enviada_en")
    private LocalDateTime enviadaEn;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @PrePersist
    protected void alCrear() { if (fechaEmision == null) fechaEmision = LocalDateTime.now(); }
}
