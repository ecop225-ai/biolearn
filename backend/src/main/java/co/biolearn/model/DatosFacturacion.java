package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Razón social, NIT y dirección con que BioLearn emite sus facturas. Cada cambio es una versión nueva (Pagos, Fase 3). */
@Entity
@Table(name = "datos_facturacion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatosFacturacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_datos_facturacion")
    private Integer id;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nit", nullable = false, length = 20)
    private String nit;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "ciudad", length = 100)
    private String ciudad;

    @Column(name = "correo", length = 150)
    private String correo;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** Quién guardó esta versión (migración 010); null en el registro inicial. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actualizo")
    private Usuario actualizadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist @PreUpdate
    protected void alGuardar() { actualizadoEn = LocalDateTime.now(); }
}
