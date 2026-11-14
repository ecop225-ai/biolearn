package co.biolearn.service;

import co.biolearn.dto.*;
import co.biolearn.model.Usuario;

import java.util.List;

/** Fase 3 del módulo de Cursos: inscripciones, acceso a las lecciones y progreso del estudiante. */
public interface InscripcionService {

    PerfilEstudianteResponse perfil(Usuario estudiante);

    List<CursoEstudianteResponse> catalogo(Usuario estudiante, String busqueda);

    CursoEstudianteResponse detalle(Integer idCurso, Usuario estudiante);

    CursoEstudianteResponse buscarPorCodigo(String codigo, Usuario estudiante);

    InscripcionResponse inscribirse(InscripcionRequest request, Usuario estudiante);

    List<InscripcionResponse> misInscripciones(Usuario estudiante);

    InscripcionResponse retirarse(Integer idInscripcion, Usuario estudiante);

    List<LeccionEstudianteResponse> lecciones(Integer idCurso, Usuario estudiante);

    LeccionEstudianteResponse leccion(Integer idLeccion, Usuario estudiante);

    AperturaMaterialResponse registrarApertura(Integer idMaterial, Usuario estudiante);

    LeccionEstudianteResponse marcarVisto(Integer idMaterial, Usuario estudiante);

    List<InscripcionResponse> inscritos(Integer idCurso, Usuario usuario);

    InscripcionResponse cambiarEstado(Integer idInscripcion, CambiarEstadoRequest request);
}
