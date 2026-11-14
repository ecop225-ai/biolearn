package co.biolearn.repository;

import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.model.SupervisorInstitucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupervisorInstitucionRepository extends JpaRepository<SupervisorInstitucion, Integer> {
    /** Representante activo de una institución (como máximo hay uno). */
    Optional<SupervisorInstitucion> findFirstByInstitucion_IdAndEstado(Integer idInstitucion, EstadoPerfilAmplio estado);

    /** Regla: una institución tiene como máximo UN representante activo. */
    boolean existsByInstitucion_IdAndEstado(Integer idInstitucion, EstadoPerfilAmplio estado);

    /** Igual que el anterior, excluyendo a un usuario (para validar la reactivación de un supervisor). */
    boolean existsByInstitucion_IdAndEstadoAndUsuario_IdNot(Integer idInstitucion, EstadoPerfilAmplio estado, Integer idUsuario);
    Optional<SupervisorInstitucion> findByUsuario_Id(Integer idUsuario);
}
