package co.biolearn.repository;

import co.biolearn.model.DatosRecaudo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatosRecaudoRepository extends JpaRepository<DatosRecaudo, Integer> {

    /** Los datos vigentes: la versión guardada más recientemente. */
    Optional<DatosRecaudo> findFirstByOrderByActualizadoEnDescIdDesc();

    List<DatosRecaudo> findAllByOrderByActualizadoEnDescIdDesc();
}
