package co.biolearn.service.impl;

import co.biolearn.dto.AreaRequest;
import co.biolearn.dto.AreaResponse;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.GradoRequest;
import co.biolearn.dto.GradoResponse;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.AreaAcademica;
import co.biolearn.model.Grado;
import co.biolearn.repository.AreaAcademicaRepository;
import co.biolearn.repository.GradoRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Catálogos de grados y áreas académicas. No existe borrado: los usan los perfiles de estudiante
 * (y más adelante los cursos), así que "eliminar" se modela como pasar el registro a 'inactivo'.
 * Un registro inactivo deja de ofrecerse en el registro de estudiantes, pero los perfiles que ya
 * lo tienen asignado lo conservan.
 */
@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private static final Set<String> NIVELES = Set.of("primaria", "secundaria", "media");
    private static final Set<String> ESTADOS = Set.of("activo", "inactivo");

    private final GradoRepository gradoRepository;
    private final AreaAcademicaRepository areaRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;

    // ── Grados ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<GradoResponse> listarGrados(boolean soloActivos) {
        List<Grado> grados = soloActivos ? gradoRepository.findByEstadoOrderByOrdenAsc("activo")
                                         : gradoRepository.findAllByOrderByOrdenAsc();
        return grados.stream().map(this::aResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GradoResponse obtenerGrado(Integer id) {
        return aResponse(buscarGrado(id));
    }

    @Override
    @Transactional
    public GradoResponse crearGrado(GradoRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del grado es obligatorio");
        }
        if (request.getNivel() == null || request.getNivel().isBlank()) {
            throw new IllegalArgumentException("El nivel del grado es obligatorio. Usa: primaria, secundaria o media");
        }
        if (request.getOrden() == null) {
            throw new IllegalArgumentException("El orden del grado es obligatorio");
        }
        String nombre = request.getNombre().trim();
        if (gradoRepository.existsByNombreIgnoreCase(nombre)) {
            throw new SolicitudInvalidaException("Ya existe un grado con ese nombre");
        }
        Grado grado = Grado.builder()
                .nombre(nombre)
                .nivel(validarNivel(request.getNivel()))
                .orden(request.getOrden())
                .descripcion(limpiar(request.getDescripcion()))
                .estado("activo")
                .build();
        return aResponse(gradoRepository.save(grado));
    }

    @Override
    @Transactional
    public GradoResponse actualizarGrado(Integer id, GradoRequest request) {
        Grado grado = buscarGrado(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            String nombre = request.getNombre().trim();
            if (gradoRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
                throw new SolicitudInvalidaException("Ya existe otro grado con ese nombre");
            }
            grado.setNombre(nombre);
        }
        if (request.getNivel() != null && !request.getNivel().isBlank()) grado.setNivel(validarNivel(request.getNivel()));
        if (request.getOrden() != null) grado.setOrden(request.getOrden());
        if (request.getDescripcion() != null) grado.setDescripcion(limpiar(request.getDescripcion()));
        return aResponse(gradoRepository.save(grado));
    }

    @Override
    @Transactional
    public GradoResponse cambiarEstadoGrado(Integer id, CambiarEstadoRequest request) {
        Grado grado = buscarGrado(id);
        grado.setEstado(validarEstado(request.getEstado()));
        return aResponse(gradoRepository.save(grado));
    }

    // ── Áreas académicas ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AreaResponse> listarAreas(boolean soloActivos) {
        List<AreaAcademica> areas = soloActivos ? areaRepository.findByEstadoOrderByNombreAsc("activo")
                                                : areaRepository.findAllByOrderByNombreAsc();
        return areas.stream().map(this::aResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResponse obtenerArea(Integer id) {
        return aResponse(buscarArea(id));
    }

    @Override
    @Transactional
    public AreaResponse crearArea(AreaRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del área es obligatorio");
        }
        String nombre = request.getNombre().trim();
        if (areaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new SolicitudInvalidaException("Ya existe un área académica con ese nombre");
        }
        AreaAcademica area = AreaAcademica.builder()
                .nombre(nombre)
                .descripcion(limpiar(request.getDescripcion()))
                .estado("activo")
                .build();
        return aResponse(areaRepository.save(area));
    }

    @Override
    @Transactional
    public AreaResponse actualizarArea(Integer id, AreaRequest request) {
        AreaAcademica area = buscarArea(id);
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            String nombre = request.getNombre().trim();
            if (areaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
                throw new SolicitudInvalidaException("Ya existe otra área académica con ese nombre");
            }
            area.setNombre(nombre);
        }
        if (request.getDescripcion() != null) area.setDescripcion(limpiar(request.getDescripcion()));
        return aResponse(areaRepository.save(area));
    }

    @Override
    @Transactional
    public AreaResponse cambiarEstadoArea(Integer id, CambiarEstadoRequest request) {
        AreaAcademica area = buscarArea(id);
        area.setEstado(validarEstado(request.getEstado()));
        return aResponse(areaRepository.save(area));
    }

    // ── auxiliares ──────────────────────────────────────────────────────────

    private Grado buscarGrado(Integer id) {
        return gradoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El grado indicado no existe"));
    }

    private AreaAcademica buscarArea(Integer id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El área académica indicada no existe"));
    }

    private static String validarNivel(String nivel) {
        String n = nivel.trim().toLowerCase();
        if (!NIVELES.contains(n)) {
            throw new IllegalArgumentException("Nivel no válido. Usa: primaria, secundaria o media");
        }
        return n;
    }

    private static String validarEstado(String estado) {
        String e = estado.trim().toLowerCase();
        if (!ESTADOS.contains(e)) {
            throw new IllegalArgumentException("Estado no válido. Usa: activo o inactivo");
        }
        return e;
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private GradoResponse aResponse(Grado g) {
        return GradoResponse.builder().id(g.getId()).nombre(g.getNombre()).nivel(g.getNivel())
                .orden(g.getOrden()).descripcion(g.getDescripcion()).estado(g.getEstado())
                .estudiantesAsociados(g.getId() != null ? perfilEstudianteRepository.countByGrado_Id(g.getId()) : 0)
                .build();
    }

    private AreaResponse aResponse(AreaAcademica a) {
        return AreaResponse.builder().id(a.getId()).nombre(a.getNombre())
                .descripcion(a.getDescripcion()).estado(a.getEstado())
                .estudiantesAsociados(a.getId() != null ? perfilEstudianteRepository.countByArea_Id(a.getId()) : 0)
                .build();
    }
}
