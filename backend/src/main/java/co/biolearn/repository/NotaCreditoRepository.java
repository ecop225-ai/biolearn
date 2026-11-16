package co.biolearn.repository;

import co.biolearn.model.NotaCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotaCreditoRepository extends JpaRepository<NotaCredito, Integer> {

    Optional<NotaCredito> findByReembolso_Id(Integer idReembolso);

    List<NotaCredito> findAllByOrderByFechaEmisionDesc();

    List<NotaCredito> findByFactura_IdOrderByFechaEmisionAsc(Integer idFactura);
}
