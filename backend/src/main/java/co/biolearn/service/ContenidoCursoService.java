package co.biolearn.service;

import co.biolearn.dto.*;
import co.biolearn.model.Usuario;

import java.util.List;

/** Fase 2 del módulo de Cursos: cursos del docente, lecciones, materiales y recursos. */
public interface ContenidoCursoService {

    List<CursoDocenteResponse> misCursos(Usuario docente);

    CursoDocenteResponse proponerCurso(CursoDocenteRequest request, Usuario docente);

    CursoDocenteResponse actualizarCursoDocente(Integer idCurso, CursoDocenteRequest request, Usuario docente);

    List<LeccionResponse> listarLecciones(Integer idCurso, Usuario usuario);

    LeccionResponse crearLeccion(Integer idCurso, LeccionRequest request, Usuario usuario);

    LeccionResponse detalleLeccion(Integer idLeccion, Usuario usuario);

    LeccionResponse actualizarLeccion(Integer idLeccion, LeccionRequest request, Usuario usuario);

    LeccionResponse cambiarEstadoLeccion(Integer idLeccion, CambiarEstadoRequest request, Usuario usuario);

    List<LeccionResponse> reordenarLecciones(Integer idCurso, OrdenLeccionesRequest request, Usuario usuario);

    LeccionResponse crearMaterial(Integer idLeccion, MaterialRequest request, Usuario usuario);

    LeccionResponse actualizarMaterial(Integer idMaterial, MaterialRequest request, Usuario usuario);

    LeccionResponse cambiarEstadoMaterial(Integer idMaterial, CambiarEstadoRequest request, Usuario usuario);

    LeccionResponse crearRecurso(Integer idMaterial, RecursoRequest request, Usuario usuario);

    LeccionResponse actualizarRecurso(Integer idRecurso, RecursoRequest request, Usuario usuario);

    LeccionResponse cambiarEstadoRecurso(Integer idRecurso, CambiarEstadoRequest request, Usuario usuario);
}
