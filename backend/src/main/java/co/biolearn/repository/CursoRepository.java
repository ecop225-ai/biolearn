package co.biolearn.repository;

import co.biolearn.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Integer> {

    List<Curso> findAllByOrderByNombreAsc();

    List<Curso> findByEstadoOrderByNombreAsc(String estado);

    /** El nombre debe ser único entre los cursos que no están archivados. */
    boolean existsByNombreIgnoreCaseAndEstadoNot(String nombre, String estado);

    boolean existsByNombreIgnoreCaseAndEstadoNotAndIdNot(String nombre, String estado, Integer id);

    /** El código del curso es único (migración 003). */
    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Integer id);
}
