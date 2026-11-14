package co.biolearn.repository;

import co.biolearn.model.HorarioCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioCursoRepository extends JpaRepository<HorarioCurso, Integer> {

    List<HorarioCurso> findByCurso_Id(Integer idCurso);

    void deleteByCurso_Id(Integer idCurso);
}
