package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

/** Último número de factura emitido en un año (migración 007). */
@Entity
@Table(name = "consecutivo_factura")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsecutivoFactura {

    @Id
    @Column(name = "anio")
    private Integer anio;

    @Column(name = "ultimo", nullable = false)
    private Integer ultimo;
}
