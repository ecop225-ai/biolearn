package co.biolearn.service;

import co.biolearn.dto.ActualizarInstitucionRequest;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.CrearInstitucionRequest;
import co.biolearn.dto.InstitucionResponse;

import java.util.List;

public interface InstitucionService {

    InstitucionResponse crear(CrearInstitucionRequest request);

    /** @param estado opcional: activa | inactiva | suspendida
     *  @param busqueda opcional: coincide con nombre, NIT o código (sin distinguir mayúsculas) */
    List<InstitucionResponse> listar(String estado, String busqueda);

    InstitucionResponse obtener(Integer id);

    InstitucionResponse actualizar(Integer id, ActualizarInstitucionRequest request);

    InstitucionResponse cambiarEstado(Integer id, CambiarEstadoRequest request);

    InstitucionResponse regenerarCodigo(Integer id);
}
