package co.biolearn.repository;

import co.biolearn.model.ReembolsoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReembolsoInscripcionRepository extends JpaRepository<ReembolsoInscripcion, Integer> {

    /** ¿El curso pagado (línea del pago) ya está en una solicitud con alguno de esos estados? */
    boolean existsByPagoInscripcion_IdAndReembolso_EstadoIn(Integer idPagoInscripcion, Collection<String> estados);
}
