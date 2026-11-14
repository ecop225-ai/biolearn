package co.biolearn.repository;

import co.biolearn.model.RecursoApoyo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecursoApoyoRepository extends JpaRepository<RecursoApoyo, Integer> {

    List<RecursoApoyo> findByMaterial_Id(Integer idMaterial);

    Optional<RecursoApoyo> findFirstByRecurso_Id(Integer idRecurso);

    Optional<RecursoApoyo> findFirstByArticulo_Id(Integer idArticulo);
}
