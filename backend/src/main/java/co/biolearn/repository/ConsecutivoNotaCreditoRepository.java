package co.biolearn.repository;

import co.biolearn.model.ConsecutivoNotaCredito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsecutivoNotaCreditoRepository extends JpaRepository<ConsecutivoNotaCredito, Integer> {

    /** Bloquea la fila del año (SELECT … FOR UPDATE) para que dos notas crédito no reciban el mismo número. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConsecutivoNotaCredito c where c.anio = :anio")
    Optional<ConsecutivoNotaCredito> bloquear(@Param("anio") Integer anio);
}
