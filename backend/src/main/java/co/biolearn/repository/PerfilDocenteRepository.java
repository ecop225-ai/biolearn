package co.biolearn.repository;

import co.biolearn.model.PerfilDocente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilDocenteRepository extends JpaRepository<PerfilDocente, Integer> {
    Optional<PerfilDocente> findByUsuario_Id(Integer idUsuario);
}
