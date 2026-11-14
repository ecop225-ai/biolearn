package co.biolearn.repository;

import co.biolearn.model.ArticuloCientifico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticuloCientificoRepository extends JpaRepository<ArticuloCientifico, Integer> {

    Optional<ArticuloCientifico> findFirstByMaterial_Id(Integer idMaterial);
}
