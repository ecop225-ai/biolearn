package co.biolearn.repository;

import co.biolearn.model.Leccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeccionRepository extends JpaRepository<Leccion, Integer> {

    List<Leccion> findByCurso_IdOrderByOrdenAsc(Integer idCurso);

    long countByCurso_Id(Integer idCurso);

    long countByCurso_IdAndEstado(Integer idCurso, String estado);

    /** ¿Alguna lección con ese estado tiene a esta como prerrequisito? */
    boolean existsByPrerequisito_IdAndEstado(Integer idPrerequisito, String estado);

    @Query("select coalesce(max(l.orden), 0) from Leccion l where l.curso.id = :idCurso")
    int maxOrden(Integer idCurso);
}
