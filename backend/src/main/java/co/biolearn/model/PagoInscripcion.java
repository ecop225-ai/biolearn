package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Línea del pago: una inscripción con el precio del curso vigente al pagar. */
@Entity
@Table(name = "pago_inscripcion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoInscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_inscripcion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Pago pago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @Column(name = "monto_asignado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoAsignado;
}
