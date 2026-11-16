package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

/** Último número de nota crédito emitido en un año (migración 008). */
@Entity
@Table(name = "consecutivo_nota_credito")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsecutivoNotaCredito {

    @Id
    @Column(name = "anio")
    private Integer anio;

    @Column(name = "ultimo", nullable = false)
    private Integer ultimo;
}
