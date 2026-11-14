package co.biolearn.service.impl;

import co.biolearn.dto.ActualizarInstitucionRequest;
import co.biolearn.dto.CambiarEstadoRequest;
import co.biolearn.dto.CrearInstitucionRequest;
import co.biolearn.dto.InstitucionResponse;
import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Institucion;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.repository.SupervisorInstitucionRepository;
import co.biolearn.service.GeneradorCodigoAfiliacion;
import co.biolearn.service.InstitucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InstitucionServiceImpl implements InstitucionService {

    private static final Set<String> ESTADOS = Set.of("activa", "inactiva", "suspendida");

    private final InstitucionRepository institucionRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final SupervisorInstitucionRepository supervisorRepository;
    private final GeneradorCodigoAfiliacion generadorCodigo;

    @Override
    @Transactional
    public InstitucionResponse crear(CrearInstitucionRequest request) {
        String nit = request.getNit().trim();
        if (institucionRepository.existsByNit(nit)) {
            throw new SolicitudInvalidaException("Ya existe una institución registrada con ese NIT");
        }
        Institucion institucion = Institucion.builder()
                .nombre(request.getNombre().trim())
                .nit(nit)
                .rector(limpiar(request.getRector()))
                .direccion(limpiar(request.getDireccion()))
                .telefono(limpiar(request.getTelefono()))
                .correo(request.getCorreo() != null && !request.getCorreo().isBlank() ? request.getCorreo().trim().toLowerCase() : null)
                .cuposContratados(request.getCuposContratados())
                .codigoAfiliacion(generadorCodigo.generarUnico())
                .estado("activa") // la registra el Administrador: queda disponible para afiliación de inmediato
                .build();
        return aResponse(institucionRepository.save(institucion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstitucionResponse> listar(String estado, String busqueda) {
        List<Institucion> instituciones;
        if (estado != null && !estado.isBlank()) {
            instituciones = institucionRepository.findByEstadoOrderByNombreAsc(validarEstado(estado));
        } else {
            instituciones = institucionRepository.findAllByOrderByNombreAsc();
        }
        String q = busqueda != null ? normalizar(busqueda) : "";
        return instituciones.stream()
                .filter(i -> q.isEmpty() || normalizar(i.getNombre() + " " + i.getNit() + " " + i.getCodigoAfiliacion()).contains(q))
                .map(this::aResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InstitucionResponse obtener(Integer id) {
        return aResponse(buscar(id));
    }

    @Override
    @Transactional
    public InstitucionResponse actualizar(Integer id, ActualizarInstitucionRequest request) {
        Institucion institucion = buscar(id);

        if (request.getNombre() != null && !request.getNombre().isBlank()) institucion.setNombre(request.getNombre().trim());
        if (request.getNit() != null && !request.getNit().isBlank()) {
            String nit = request.getNit().trim();
            if (institucionRepository.existsByNitAndIdNot(nit, id)) {
                throw new SolicitudInvalidaException("Ya existe otra institución registrada con ese NIT");
            }
            institucion.setNit(nit);
        }
        if (request.getRector() != null) institucion.setRector(limpiar(request.getRector()));
        if (request.getDireccion() != null) institucion.setDireccion(limpiar(request.getDireccion()));
        if (request.getTelefono() != null) institucion.setTelefono(limpiar(request.getTelefono()));
        if (request.getCorreo() != null) {
            institucion.setCorreo(request.getCorreo().isBlank() ? null : request.getCorreo().trim().toLowerCase());
        }
        if (request.getCuposContratados() != null) {
            long usados = cuposUsados(id);
            if (request.getCuposContratados() < usados) {
                throw new SolicitudInvalidaException("No puedes asignar " + request.getCuposContratados()
                        + " cupos: la institución ya tiene " + usados + " estudiantes activos afiliados");
            }
            institucion.setCuposContratados(request.getCuposContratados());
        }
        return aResponse(institucionRepository.save(institucion));
    }

    @Override
    @Transactional
    public InstitucionResponse cambiarEstado(Integer id, CambiarEstadoRequest request) {
        Institucion institucion = buscar(id);
        // Inactivar o suspender no afecta a los estudiantes ya afiliados: solo impide nuevas afiliaciones
        // (lo valida el registro) y la asignación de nuevos representantes (lo valida el módulo de Usuarios).
        institucion.setEstado(validarEstado(request.getEstado()));
        return aResponse(institucionRepository.save(institucion));
    }

    @Override
    @Transactional
    public InstitucionResponse regenerarCodigo(Integer id) {
        Institucion institucion = buscar(id);
        String anterior = institucion.getCodigoAfiliacion();
        String nuevo;
        do {
            nuevo = generadorCodigo.generarUnico();
        } while (nuevo.equals(anterior));
        // El código anterior deja de funcionar de inmediato; los estudiantes ya afiliados no se ven afectados.
        institucion.setCodigoAfiliacion(nuevo);
        return aResponse(institucionRepository.save(institucion));
    }

    // ── auxiliares ──────────────────────────────────────────────────────────

    private Institucion buscar(Integer id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La institución indicada no existe"));
    }

    private String validarEstado(String estado) {
        String e = estado.trim().toLowerCase();
        if (!ESTADOS.contains(e)) {
            throw new IllegalArgumentException("Estado de institución no válido. Usa: activa, inactiva o suspendida");
        }
        return e;
    }

    private long cuposUsados(Integer idInstitucion) {
        return perfilEstudianteRepository.countByInstitucion_IdAndEstado(idInstitucion, EstadoEstudiante.activo);
    }

    private static String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private InstitucionResponse aResponse(Institucion i) {
        long usados = i.getId() != null ? cuposUsados(i.getId()) : 0;
        Integer disponibles = i.getCuposContratados() != null
                ? (int) Math.max(0, i.getCuposContratados() - usados) : null;

        InstitucionResponse.RepresentanteResumen representante = i.getId() == null ? null :
                supervisorRepository.findFirstByInstitucion_IdAndEstado(i.getId(), EstadoPerfilAmplio.activo)
                        .map(s -> InstitucionResponse.RepresentanteResumen.builder()
                                .idUsuario(s.getUsuario().getId())
                                .nombreCompleto(s.getUsuario().getNombre() + " " + s.getUsuario().getApellido())
                                .correo(s.getUsuario().getCorreo())
                                .cargo(s.getCargo())
                                .estadoCuenta(s.getUsuario().getEstado().name())
                                .build())
                        .orElse(null);

        return InstitucionResponse.builder()
                .id(i.getId())
                .nombre(i.getNombre())
                .nit(i.getNit())
                .rector(i.getRector())
                .direccion(i.getDireccion())
                .telefono(i.getTelefono())
                .correo(i.getCorreo())
                .codigoAfiliacion(i.getCodigoAfiliacion())
                .cuposContratados(i.getCuposContratados())
                .cuposUsados(usados)
                .cuposDisponibles(disponibles)
                .estado(i.getEstado())
                .fechaRegistro(i.getCreadoEn() != null ? i.getCreadoEn().toLocalDate() : null)
                .representante(representante)
                .build();
    }
}
