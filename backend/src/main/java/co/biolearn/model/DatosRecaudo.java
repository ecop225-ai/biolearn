package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Cuenta bancaria y Nequi donde el estudiante transfiere (Pagos, Fase 3). Cada cambio guarda una versión nueva;
 * la vigente es la actualizada más recientemente.
 */
@Entity
@Table(name = "datos_recaudo")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DatosRecaudo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_datos_recaudo")
    private Integer id;

    @Column(name = "banco", nullable = false, length = 80)
    private String banco;

    @Column(name = "tipo_cuenta", nullable = false, length = 20)
    private String tipoCuenta;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String numeroCuenta;

    @Column(name = "titular", nullable = false, length = 150)
    private String titular;

    @Column(name = "documento_titular", nullable = false, length = 20)
    private String documentoTitular;

    @Column(name = "telefono_nequi", length = 10)
    private String telefonoNequi;

    @Column(name = "instrucciones", length = 500)
    private String instrucciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actualizo")
    private Usuario actualizadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist @PreUpdate
    protected void alGuardar() { actualizadoEn = LocalDateTime.now(); }
}
