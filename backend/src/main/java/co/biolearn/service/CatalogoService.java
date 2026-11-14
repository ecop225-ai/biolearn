package co.biolearn.service;

import co.biolearn.dto.AreaRequest;
import co.biolearn.dto.AreaResponse;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.GradoRequest;
import co.biolearn.dto.GradoResponse;

import java.util.List;

public interface CatalogoService {

    /** @param soloActivos true para los listados públicos (formulario de registro) */
    List<GradoResponse> listarGrados(boolean soloActivos);

    GradoResponse obtenerGrado(Integer id);

    GradoResponse crearGrado(GradoRequest request);

    GradoResponse actualizarGrado(Integer id, GradoRequest request);

    GradoResponse cambiarEstadoGrado(Integer id, CambiarEstadoRequest request);

    List<AreaResponse> listarAreas(boolean soloActivos);

    AreaResponse obtenerArea(Integer id);

    AreaResponse crearArea(AreaRequest request);

    AreaResponse actualizarArea(Integer id, AreaRequest request);

    AreaResponse cambiarEstadoArea(Integer id, CambiarEstadoRequest request);
}
