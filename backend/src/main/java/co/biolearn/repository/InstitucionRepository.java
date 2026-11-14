package co.biolearn.repository;

import co.biolearn.model.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstitucionRepository extends JpaRepository<Institucion, Integer> {
    Optional<Institucion> findByCodigoAfiliacionIgnoreCase(String codigoAfiliacion);

    boolean existsByCodigoAfiliacion(String codigoAfiliacion);

    boolean existsByNit(String nit);

    boolean existsByNitAndIdNot(String nit, Integer id);

    List<Institucion> findAllByOrderByNombreAsc();

    List<Institucion> findByEstadoOrderByNombreAsc(String estado);
}
