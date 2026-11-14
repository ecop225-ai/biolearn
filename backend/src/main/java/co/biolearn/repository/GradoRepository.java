package co.biolearn.repository;

import co.biolearn.model.Grado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradoRepository extends JpaRepository<Grado, Integer> {
    List<Grado> findAllByOrderByOrdenAsc();

    List<Grado> findByEstadoOrderByOrdenAsc(String estado);

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
