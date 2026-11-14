package co.biolearn.repository;

import co.biolearn.model.PerfilAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilAdminRepository extends JpaRepository<PerfilAdmin, Integer> {

    java.util.Optional<PerfilAdmin> findByUsuario_Id(Integer idUsuario);
}
