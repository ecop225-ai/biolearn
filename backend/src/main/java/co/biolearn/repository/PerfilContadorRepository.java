package co.biolearn.repository;

import co.biolearn.model.PerfilContador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilContadorRepository extends JpaRepository<PerfilContador, Integer> {
    Optional<PerfilContador> findByUsuario_Id(Integer idUsuario);
}
