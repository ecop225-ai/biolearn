package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Razón social, NIT y dirección con que BioLearn emite sus facturas. */
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

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist @PreUpdate
    protected void alGuardar() { actualizadoEn = LocalDateTime.now(); }
}
