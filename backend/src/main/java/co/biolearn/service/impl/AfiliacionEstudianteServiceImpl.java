package co.biolearn.service.impl;

import co.biolearn.dto.PerfilEstudianteResponse;
import co.biolearn.dto.UnirseInstitucionRequest;
import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Institucion;
import co.biolearn.model.Notificacion;
import co.biolearn.model.PerfilEstudiante;
import co.biolearn.model.Usuario;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.NotificacionRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import co.biolearn.service.AfiliacionEstudianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Institución del estudiante («Mi institución» en Mi perfil y Usuarios del Administrador).
 * - Unirse o cambiar de colegio con el código de afiliación del nuevo colegio: mismas reglas del registro
 *   (código válido, institución activa y cupos disponibles). El código prueba que el nuevo colegio lo autoriza.
 * - Salir del colegio: queda como estudiante independiente y libera su cupo.
 * - El Administrador puede asignar, cambiar o quitar la institución (casos especiales: código perdido, error).
 * El estudiante conserva siempre sus cursos (los paga él, aunque haya entrado con un código del colegio), su progreso,
 * sus pagos y sus certificados. Las inscripciones guardan el vínculo con el colegio con que se hicieron (historial).
 */
@Service
@RequiredArgsConstructor
public class AfiliacionEstudianteServiceImpl implements AfiliacionEstudianteService {

    static final String CONSERVA = "Conservas tus cursos, tu progreso, tus pagos y tus certificados.";

    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final InstitucionRepository institucionRepository;
    private final NotificacionRepository notificacionRepository;

    @Override
    @Transactional
    public PerfilEstudianteResponse unirse(UnirseInstitucionRequest request, Usuario usuario) {
        PerfilEstudiante est = perfilEstudianteRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los estudiantes"));
        Institucion inst = institucionRepository.findByCodigoAfiliacionIgnoreCase(request.getCodigoAfiliacion().trim())
                .orElseThrow(() -> new SolicitudInvalidaException("El código de institución no es válido. Verifícalo con tu colegio."));
        Institucion anterior = est.getInstitucion();
        if (anterior != null && anterior.getId().equals(inst.getId()))
            throw new SolicitudInvalidaException("Ya perteneces a «" + inst.getNombre() + "»");
        validarDisponible(inst);
        vincular(est, inst);
        notificar(usuario, null, inst.getId(), anterior == null ? "Te uniste a " + inst.getNombre() : "Cambiaste a " + inst.getNombre(),
                (anterior == null ? "" : "Ya no perteneces a " + anterior.getNombre() + ". ")
                        + "Ahora ves los cursos de tu colegio y puedes usar sus códigos de inscripción. " + CONSERVA);
        return aPerfil(est);
    }

    @Override
    @Transactional
    public PerfilEstudianteResponse salir(Usuario usuario) {
        PerfilEstudiante est = perfilEstudianteRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los estudiantes"));
        Institucion anterior = est.getInstitucion();
        if (anterior == null) throw new SolicitudInvalidaException("No perteneces a ningún colegio");
        vincular(est, null);
        notificar(usuario, null, anterior.getId(), "Saliste de " + anterior.getNombre(),
                "Ahora eres estudiante independiente. " + CONSERVA + " Puedes unirte a un colegio cuando quieras con su código de afiliación.");
        return aPerfil(est);
    }

    @Override
    @Transactional
    public PerfilEstudianteResponse asignarPorAdministrador(Integer idUsuario, Integer idInstitucion, Usuario administrador) {
        PerfilEstudiante est = perfilEstudianteRepository.findByUsuario_Id(idUsuario)
                .orElseThrow(() -> new SolicitudInvalidaException("Solo se puede cambiar así la institución de un estudiante"));
        Institucion anterior = est.getInstitucion();
        Usuario u = est.getUsuario();
        if (idInstitucion == null) {
            if (anterior == null) throw new SolicitudInvalidaException("El estudiante ya es independiente");
            vincular(est, null);
            notificar(u, administrador, anterior.getId(), "Ya no perteneces a " + anterior.getNombre(),
                    "El administrador te dejó como estudiante independiente. " + CONSERVA);
            return aPerfil(est);
        }
        Institucion inst = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La institución indicada no existe"));
        if (anterior != null && anterior.getId().equals(inst.getId()))
            throw new SolicitudInvalidaException("El estudiante ya pertenece a esa institución");
        validarDisponible(inst);
        vincular(est, inst);
        notificar(u, administrador, inst.getId(), "Ahora perteneces a " + inst.getNombre(),
                "El administrador te vinculó a " + inst.getNombre() + (anterior != null ? " (antes " + anterior.getNombre() + ")" : "") + ". " + CONSERVA);
        return aPerfil(est);
    }

    /** Institución activa y con cupo (los cupos cuentan a los estudiantes activos vinculados). */
    private void validarDisponible(Institucion inst) {
        if (!"activa".equalsIgnoreCase(inst.getEstado()))
            throw new SolicitudInvalidaException("Esta institución no está activa en este momento");
        Integer cupos = inst.getCuposContratados();
        if (cupos != null && perfilEstudianteRepository.countByInstitucion_IdAndEstado(inst.getId(), EstadoEstudiante.activo) >= cupos)
            throw new SolicitudInvalidaException("Tu institución no tiene cupos disponibles en este momento. Comunícate con tu colegio.");
    }

    private void vincular(PerfilEstudiante est, Institucion inst) {
        est.setInstitucion(inst);
        perfilEstudianteRepository.save(est);
    }

    private void notificar(Usuario destino, Usuario emisor, Integer idInstitucion, String titulo, String contenido) {
        notificacionRepository.save(Notificacion.builder().usuario(destino).emisor(emisor).tipo("sistema").entidadId(idInstitucion)
                .titulo(titulo).contenido(contenido).build());
    }

    private static PerfilEstudianteResponse aPerfil(PerfilEstudiante est) {
        Usuario u = est.getUsuario();
        Institucion inst = est.getInstitucion();
        return PerfilEstudianteResponse.builder()
                .idEstudiante(est.getId()).nombre(u.getNombre() + " " + u.getApellido()).correo(u.getCorreo())
                .idInstitucion(inst != null ? inst.getId() : null).institucion(inst != null ? inst.getNombre() : null)
                .grado(est.getGrado() != null ? est.getGrado().getNombre() : null)
                .area(est.getArea() != null ? est.getArea().getNombre() : null)
                .build();
    }
}
