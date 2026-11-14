package co.biolearn.repository;

import co.biolearn.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {

    List<Notificacion> findByUsuario_IdOrderByCreadaEnDesc(Integer idUsuario);
}
