package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Curso incluido en una solicitud de reembolso, con el valor pagado y el avance al solicitar. */
@Entity
@Table(name = "reembolso_inscripcion")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReembolsoInscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reembolso_inscripcion")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reembolso", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Reembolso reembolso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pago_inscripcion", nullable = false)
    private PagoInscripcion pagoInscripcion;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "avance", nullable = false, precision = 5, scale = 2)
    private BigDecimal avance;

    /** activa o retirada: estado al que vuelve la inscripción si la solicitud se rechaza o se cancela. */
    @Column(name = "estado_inscripcion_previo", nullable = false, length = 20)
    private String estadoInscripcionPrevio;
}
