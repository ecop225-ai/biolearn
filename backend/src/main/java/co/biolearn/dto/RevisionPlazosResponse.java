package co.biolearn.dto;

import lombok.*;

import java.time.LocalDateTime;

/** Resultado de la revisión de plazos de acceso a los cursos (diaria o pedida por el Administrador). */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RevisionPlazosResponse {
    private LocalDateTime revisadoEn;
    /** Inscripciones activas cuyo plazo para terminar venció. */
    private int vencidas;
    /** Inscripciones completadas cuyo repaso terminó. */
    private int cerradas;
    /** Avisos de 15 días enviados: plazo para terminar y fin del repaso. */
    private int avisosVencimiento;
    private int avisosRepaso;
}
