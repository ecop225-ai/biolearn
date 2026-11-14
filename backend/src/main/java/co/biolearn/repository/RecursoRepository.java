package co.biolearn.repository;

import co.biolearn.model.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RecursoRepository extends JpaRepository<Recurso, Integer> {

    List<Recurso> findByMaterial_IdOrderByOrdenAsc(Integer idMaterial);

    @Query("select coalesce(max(r.orden), 0) from Recurso r where r.material.id = :idMaterial")
    int maxOrden(Integer idMaterial);
}
