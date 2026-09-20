package co.biolearn.repository;

import co.biolearn.model.VerificacionCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificacionCuentaRepository extends JpaRepository<VerificacionCuenta, Integer> {
    Optional<VerificacionCuenta> findByToken(String token);
}
