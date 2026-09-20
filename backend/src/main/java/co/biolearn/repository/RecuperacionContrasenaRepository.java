package co.biolearn.repository;

import co.biolearn.model.RecuperacionContrasena;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecuperacionContrasenaRepository extends JpaRepository<RecuperacionContrasena, Integer> {
    Optional<RecuperacionContrasena> findByToken(String token);
}
