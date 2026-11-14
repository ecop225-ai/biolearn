package co.biolearn.repository;

import co.biolearn.model.MaterialContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MaterialContenidoRepository extends JpaRepository<MaterialContenido, Integer> {

    List<MaterialContenido> findByLeccion_IdOrderByOrdenAsc(Integer idLeccion);

    long countByLeccion_Id(Integer idLeccion);

    long countByLeccion_IdAndEstado(Integer idLeccion, String estado);

    @Query("select coalesce(max(m.orden), 0) from MaterialContenido m where m.leccion.id = :idLeccion")
    int maxOrden(Integer idLeccion);
}
