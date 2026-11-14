package co.biolearn.repository;

import co.biolearn.model.DatosFacturacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatosFacturacionRepository extends JpaRepository<DatosFacturacion, Integer> {

    /** Los datos vigentes: el registro actualizado más recientemente. */
    Optional<DatosFacturacion> findFirstByOrderByActualizadoEnDescIdDesc();
}
