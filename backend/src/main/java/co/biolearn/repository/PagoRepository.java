package co.biolearn.repository;

import co.biolearn.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByEstudiante_IdOrderByCreadoEnDesc(Integer idEstudiante);

    List<Pago> findAllByOrderByCreadoEnDesc();
}
