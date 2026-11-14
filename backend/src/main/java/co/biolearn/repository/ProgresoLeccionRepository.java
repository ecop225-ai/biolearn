package co.biolearn.repository;

import co.biolearn.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProgresoLeccionRepository extends JpaRepository<ProgresoLeccion, Integer> {

    Optional<ProgresoLeccion> findByInscripcion_IdAndLeccion_Id(Integer idInscripcion, Integer idLeccion);

    List<ProgresoLeccion> findByInscripcion_Id(Integer idInscripcion);
}
