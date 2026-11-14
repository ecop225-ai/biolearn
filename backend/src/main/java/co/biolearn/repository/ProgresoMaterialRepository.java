package co.biolearn.repository;

import co.biolearn.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProgresoMaterialRepository extends JpaRepository<ProgresoMaterial, Integer> {

    Optional<ProgresoMaterial> findByInscripcion_IdAndMaterial_Id(Integer idInscripcion, Integer idMaterial);

    /** Materiales activos de la lección que el estudiante ya vio. */
    @Query("select count(p) from ProgresoMaterial p where p.inscripcion.id = :idInscripcion and p.material.leccion.id = :idLeccion and p.material.estado = 'activo' and p.vistoEn is not null")
    long contarVistosActivos(Integer idInscripcion, Integer idLeccion);

    @Query("select p.material.id from ProgresoMaterial p where p.inscripcion.id = :idInscripcion and p.vistoEn is not null")
    List<Integer> idsVistos(Integer idInscripcion);
}
