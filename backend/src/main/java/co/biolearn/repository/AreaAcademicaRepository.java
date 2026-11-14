package co.biolearn.repository;

import co.biolearn.model.AreaAcademica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AreaAcademicaRepository extends JpaRepository<AreaAcademica, Integer> {
    List<AreaAcademica> findAllByOrderByNombreAsc();

    List<AreaAcademica> findByEstadoOrderByNombreAsc(String estado);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
