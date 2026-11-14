package co.biolearn.repository;

import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.model.PerfilEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PerfilEstudianteRepository extends JpaRepository<PerfilEstudiante, Integer> {
    Optional<PerfilEstudiante> findByUsuario_Id(Integer idUsuario);

    /** Cupos usados de una institución = estudiantes afiliados con perfil activo. */
    long countByInstitucion_IdAndEstado(Integer idInstitucion, EstadoEstudiante estado);

    /** Estudiantes que tienen asignado un grado o un área (para el detalle de los catálogos). */
    long countByGrado_Id(Integer idGrado);

    long countByArea_Id(Integer idArea);
}
