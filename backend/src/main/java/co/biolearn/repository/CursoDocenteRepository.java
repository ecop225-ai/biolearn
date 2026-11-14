package co.biolearn.repository;

import co.biolearn.model.CursoDocente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CursoDocenteRepository extends JpaRepository<CursoDocente, Integer> {

    List<CursoDocente> findByCurso_IdOrderByAsignadoEnAsc(Integer idCurso);

    Optional<CursoDocente> findByCurso_IdAndDocente_Id(Integer idCurso, Integer idDocente);

    long countByCurso_IdAndEstado(Integer idCurso, String estado);

    List<CursoDocente> findByDocente_IdAndEstado(Integer idDocente, String estado);
}
