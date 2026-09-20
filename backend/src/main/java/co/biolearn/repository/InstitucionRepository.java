package co.biolearn.repository;

import co.biolearn.model.Institucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstitucionRepository extends JpaRepository<Institucion, Integer> {
    Optional<Institucion> findByCodigoAfiliacionIgnoreCase(String codigoAfiliacion);
}
