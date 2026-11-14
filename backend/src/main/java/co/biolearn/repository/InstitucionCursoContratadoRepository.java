package co.biolearn.repository;

import co.biolearn.model.InstitucionCursoContratado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstitucionCursoContratadoRepository extends JpaRepository<InstitucionCursoContratado, Integer> {

    List<InstitucionCursoContratado> findByCurso_IdOrderByContratadoEnAsc(Integer idCurso);

    Optional<InstitucionCursoContratado> findByCurso_IdAndInstitucion_Id(Integer idCurso, Integer idInstitucion);

    boolean existsByCurso_IdAndInstitucion_Id(Integer idCurso, Integer idInstitucion);

    long countByCurso_Id(Integer idCurso);

    boolean existsByCodigoInscripcion(String codigo);

    Optional<InstitucionCursoContratado> findByCodigoInscripcion(String codigo);
}
