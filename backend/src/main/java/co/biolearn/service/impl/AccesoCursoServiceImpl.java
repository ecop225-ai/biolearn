package co.biolearn.service.impl;

import co.biolearn.dto.RevisionPlazosResponse;
import co.biolearn.model.Inscripcion;
import co.biolearn.model.Notificacion;
import co.biolearn.model.Usuario;
import co.biolearn.repository.InscripcionRepository;
import co.biolearn.repository.NotificacionRepository;
import co.biolearn.service.AccesoCursoService;
import co.biolearn.service.CorreoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Revisión de los plazos de acceso (migración 011). Se ejecuta todos los días (biolearn.acceso.revision-cron,
 * 1:30 a. m. por defecto) y el Administrador puede pedirla en cualquier momento.
 * - activa con acceso_hasta pasado → vencida, con notificación y correo.
 * - completada con repaso_hasta pasado → cerrada, con notificación y correo.
 * - Faltando 15 días o menos para cualquiera de las dos fechas, un aviso (una sola vez por plazo).
 * El acceso se controla también con las fechas en cada consulta (InscripcionServiceImpl.estadoVigente), así que
 * la hora de la revisión no abre ni cierra el contenido: solo guarda el estado y avisa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccesoCursoServiceImpl implements AccesoCursoService {

    static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InscripcionRepository inscripcionRepository;
    private final NotificacionRepository notificacionRepository;
    private final CorreoService correoService;

    @Scheduled(cron = "${biolearn.acceso.revision-cron:0 30 1 * * *}")
    public void revisionDiaria() {
        RevisionPlazosResponse r = revisar(LocalDateTime.now());
        log.info("Revisión de plazos de los cursos: {} vencidas, {} cerradas, {} avisos de vencimiento y {} de repaso",
                r.getVencidas(), r.getCerradas(), r.getAvisosVencimiento(), r.getAvisosRepaso());
    }

    @Override
    @Transactional
    public RevisionPlazosResponse revisar(LocalDateTime ahora) {
        LocalDateTime enQuince = ahora.plusDays(DIAS_AVISO);

        List<Inscripcion> vencidas = inscripcionRepository.findByEstadoAndAccesoHastaBefore("activa", ahora);
        vencidas.forEach(i -> {
            i.setEstado("vencida");
            inscripcionRepository.save(i);
            avisar(i, "Venció el plazo de «" + i.getCurso().getNombre() + "»",
                    "El plazo para terminar «" + i.getCurso().getNombre() + "» venció el " + i.getAccesoHasta().format(FECHA)
                            + ". Conservas tu avance: renueva el curso desde «Mis cursos» para seguir donde ibas.");
        });

        List<Inscripcion> cerradas = inscripcionRepository.findByEstadoAndRepasoHastaBefore("completada", ahora);
        cerradas.forEach(i -> {
            i.setEstado("cerrada");
            inscripcionRepository.save(i);
            avisar(i, "Terminó el repaso de «" + i.getCurso().getNombre() + "»",
                    "El repaso de «" + i.getCurso().getNombre() + "» terminó el " + i.getRepasoHasta().format(FECHA)
                            + ". Conservas tu certificado, tus notas y tu historial.");
        });

        List<Inscripcion> porVencer = inscripcionRepository.findByEstadoAndAccesoHastaBetweenAndAvisoVencimientoEnIsNull("activa", ahora, enQuince);
        porVencer.forEach(i -> {
            i.setAvisoVencimientoEn(ahora);
            inscripcionRepository.save(i);
            avisar(i, "Te quedan " + dias(ahora, i.getAccesoHasta()) + " días para terminar «" + i.getCurso().getNombre() + "»",
                    "Tienes hasta el " + i.getAccesoHasta().format(FECHA) + " para terminar «" + i.getCurso().getNombre()
                            + "». Después el curso se cierra; conservarás tu avance y podrás renovarlo.");
        });

        List<Inscripcion> repasoPorTerminar = inscripcionRepository.findByEstadoAndRepasoHastaBetweenAndAvisoRepasoEnIsNull("completada", ahora, enQuince);
        repasoPorTerminar.forEach(i -> {
            i.setAvisoRepasoEn(ahora);
            inscripcionRepository.save(i);
            avisar(i, "Te quedan " + dias(ahora, i.getRepasoHasta()) + " días de repaso en «" + i.getCurso().getNombre() + "»",
                    "Puedes repasar «" + i.getCurso().getNombre() + "» hasta el " + i.getRepasoHasta().format(FECHA)
                            + ". Después se cierra el contenido; conservas tu certificado y tus notas.");
        });

        return RevisionPlazosResponse.builder().revisadoEn(ahora)
                .vencidas(vencidas.size()).cerradas(cerradas.size())
                .avisosVencimiento(porVencer.size()).avisosRepaso(repasoPorTerminar.size()).build();
    }

    static long dias(LocalDateTime ahora, LocalDateTime limite) {
        return Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(ahora.toLocalDate(), limite.toLocalDate()));
    }

    private void avisar(Inscripcion i, String titulo, String mensaje) {
        Usuario u = i.getEstudiante().getUsuario();
        notificacionRepository.save(Notificacion.builder().usuario(u).tipo("inscripcion").entidadId(i.getId())
                .titulo(titulo.length() > 200 ? titulo.substring(0, 199) + "…" : titulo).contenido(mensaje).build());
        correoService.avisoCurso(u.getCorreo(), u.getNombre(), titulo, HtmlUtils.htmlEscape(mensaje));
    }
}
