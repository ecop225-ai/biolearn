package co.biolearn.repository;

import co.biolearn.model.PerfilEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilEstudianteRepository extends JpaRepository<PerfilEstudiante, Integer> {
    Optional<PerfilEstudiante> findByUsuario_Id(Integer idUsuario);
}
