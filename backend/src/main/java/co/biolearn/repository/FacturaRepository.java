package co.biolearn.repository;

import co.biolearn.model.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {

    Optional<Factura> findByPago_Id(Integer idPago);

    List<Factura> findAllByOrderByFechaEmisionDesc();

    List<Factura> findByEstudiante_IdOrderByFechaEmisionDesc(Integer idEstudiante);
}
