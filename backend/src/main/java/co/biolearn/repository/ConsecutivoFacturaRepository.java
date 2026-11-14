package co.biolearn.repository;

import co.biolearn.model.ConsecutivoFactura;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConsecutivoFacturaRepository extends JpaRepository<ConsecutivoFactura, Integer> {

    /** Bloquea la fila del año (SELECT … FOR UPDATE) para que dos facturas no reciban el mismo número. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConsecutivoFactura c where c.anio = :anio")
    Optional<ConsecutivoFactura> bloquear(@Param("anio") Integer anio);
}
