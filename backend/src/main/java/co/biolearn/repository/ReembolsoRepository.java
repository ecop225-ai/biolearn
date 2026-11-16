package co.biolearn.repository;

import co.biolearn.model.Reembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

import java.util.List;

public interface ReembolsoRepository extends JpaRepository<Reembolso, Integer> {

    List<Reembolso> findAllByOrderBySolicitadoEnDesc();

    List<Reembolso> findBySolicitante_IdOrderBySolicitadoEnDesc(Integer idUsuario);

    List<Reembolso> findByPago_Id(Integer idPago);

    List<Reembolso> findBySolicitadoEnBetweenOrderBySolicitadoEnAsc(LocalDateTime desde, LocalDateTime hasta);

    /** Reembolsos devueltos dentro del período (lo que salió de caja). */
    List<Reembolso> findByEstadoAndDevueltoEnBetween(String estado, LocalDateTime desde, LocalDateTime hasta);

    List<Reembolso> findByEstado(String estado);
}
