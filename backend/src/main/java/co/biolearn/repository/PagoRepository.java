package co.biolearn.repository;

import co.biolearn.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.time.LocalDateTime;

import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByEstudiante_IdOrderByCreadoEnDesc(Integer idEstudiante);

    List<Pago> findAllByOrderByCreadoEnDesc();

    /** Pagos en ciertos estados con fecha de pago dentro del período (reportes financieros). */
    List<Pago> findByEstadoInAndFechaPagoBetweenOrderByFechaPagoAsc(Collection<String> estados, LocalDateTime desde, LocalDateTime hasta);

    List<Pago> findByEstadoOrderByCreadoEnDesc(String estado);
}
