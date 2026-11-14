package co.biolearn.service;

import co.biolearn.dto.*;

import java.util.List;

public interface CursoService {

    List<CursoResponse> listar(String estado, Integer idArea, Integer idGrado, Integer idDocente, String busqueda);

    CursoResponse obtener(Integer id);

    CursoResponse crear(CrearCursoRequest request, Integer idUsuarioAdmin);

    CursoResponse actualizar(Integer id, ActualizarCursoRequest request);

    CursoResponse cambiarEstado(Integer id, CambiarEstadoRequest request);

    CursoResponse agregarDocente(Integer id, AgregarDocenteCursoRequest request);

    CursoResponse retirarDocente(Integer id, Integer idDocente);

    CursoResponse definirHorarios(Integer id, HorariosCursoRequest request);

    CursoResponse contratar(Integer id, ContratarCursoRequest request);

    CursoResponse quitarContratacion(Integer id, Integer idInstitucion);

    List<CursoCatalogoResponse> catalogo(Integer idArea, Integer idGrado, String busqueda);

    CursoCatalogoResponse detalleCatalogo(Integer id);

    /** Fase 3: genera un código de inscripción nuevo para la contratación. */
    CursoResponse regenerarCodigo(Integer id, Integer idInstitucion);
}
