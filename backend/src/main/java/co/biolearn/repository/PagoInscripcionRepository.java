package co.biolearn.repository;

import co.biolearn.model.PagoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PagoInscripcionRepository extends JpaRepository<PagoInscripcion, Integer> {

    /** ¿La inscripción está en un pago con alguno de esos estados? (p. ej. un pago pendiente en proceso). */
    boolean existsByInscripcion_IdAndPago_EstadoIn(Integer idInscripcion, Collection<String> estados);

    List<PagoInscripcion> findByPago_Id(Integer idPago);
}
