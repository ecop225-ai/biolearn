package co.biolearn.repository;

import co.biolearn.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {

    Optional<Inscripcion> findByCurso_IdAndEstudiante_Id(Integer idCurso, Integer idEstudiante);

    List<Inscripcion> findByEstudiante_IdOrderByFechaInscripcionDesc(Integer idEstudiante);

    List<Inscripcion> findByCurso_IdOrderByFechaInscripcionAsc(Integer idCurso);

    boolean existsByContratacion_IdAndEstadoIn(Integer idContratacion, Collection<String> estados);

    long countByContratacion_IdAndEstadoIn(Integer idContratacion, Collection<String> estados);
}
