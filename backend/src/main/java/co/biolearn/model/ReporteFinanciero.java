package co.biolearn.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Registro de cada reporte financiero generado: tipo, período, formato, motivo y quién lo generó (Pagos, Fase 3). */
@Entity
@Table(name = "reporte_financiero")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReporteFinanciero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte_financiero")
    private Integer id;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "desde", nullable = false)
    private LocalDate desde;

    @Column(name = "hasta", nullable = false)
    private LocalDate hasta;

    @Column(name = "formato", nullable = false, length = 10)
    private String formato;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "generado_en", nullable = false, updatable = false)
    private LocalDateTime generadoEn;

    @PrePersist
    protected void alCrear() { if (generadoEn == null) generadoEn = LocalDateTime.now(); }
}
