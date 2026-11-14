package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Factura emitida automáticamente al aprobarse un pago (una por pago).
 * Guarda los datos de facturación vigentes al emitirla (RNF-021).
 */
@Entity
@Table(name = "factura")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private PerfilEstudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_datos_facturacion")
    private DatosFacturacion datosFacturacion;

    @Column(name = "numero_factura", nullable = false, unique = true, length = 50)
    private String numero;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado", precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private String estado = "emitida";

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
