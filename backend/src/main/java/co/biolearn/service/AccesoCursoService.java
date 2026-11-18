package co.biolearn.service;

import co.biolearn.dto.RevisionPlazosResponse;

import java.time.LocalDateTime;

/** Acceso a los cursos por tiempo: vence plazos, cierra repasos y envía los avisos de 15 días. */
public interface AccesoCursoService {

    int DIAS_AVISO = 15;

    RevisionPlazosResponse revisar(LocalDateTime ahora);
}
