package co.biolearn.repository;

import co.biolearn.model.ReporteFinanciero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReporteFinancieroRepository extends JpaRepository<ReporteFinanciero, Integer> {

    List<ReporteFinanciero> findTop50ByOrderByGeneradoEnDescIdDesc();
}
