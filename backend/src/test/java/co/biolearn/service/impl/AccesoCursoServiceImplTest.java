package co.biolearn.service.impl;

import co.biolearn.dto.RevisionPlazosResponse;
import co.biolearn.model.*;
import co.biolearn.repository.InscripcionRepository;
import co.biolearn.repository.NotificacionRepository;
import co.biolearn.service.CorreoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AccesoCursoServiceImpl — revisión diaria de plazos y avisos de 15 días")
class AccesoCursoServiceImplTest {

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private CorreoService correoService;
    @InjectMocks private AccesoCursoServiceImpl servicio;

    private final LocalDateTime ahora = LocalDateTime.of(2026, 11, 17, 1, 30);

    private Inscripcion insc(int id, String estado) {
        Usuario u = Usuario.builder().id(30).nombre("Esteban").apellido("Valencia").correo("e@t.co").build();
        Curso c = Curso.builder().id(10).nombre("Ecología general").build();
        return Inscripcion.builder().id(id).curso(c).estudiante(PerfilEstudiante.builder().id(9).usuario(u).build()).estado(estado).build();
    }

    private List<Notificacion> notificaciones() {
        ArgumentCaptor<Notificacion> n = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, atLeastOnce()).save(n.capture());
        return n.getAllValues();
    }

    @Test @DisplayName("Plazo para terminar vencido → vencida, con notificación y correo que invitan a renovar")
    void vence() {
        Inscripcion v = insc(1, "activa"); v.setAccesoHasta(ahora.minusDays(1));
        when(inscripcionRepository.findByEstadoAndAccesoHastaBefore("activa", ahora)).thenReturn(List.of(v));
        RevisionPlazosResponse r = servicio.revisar(ahora);
        assertThat(r.getVencidas()).isEqualTo(1);
        assertThat(v.getEstado()).isEqualTo("vencida");
        Notificacion n = notificaciones().get(0);
        assertThat(n.getTipo()).isEqualTo("inscripcion");
        assertThat(n.getEntidadId()).isEqualTo(1);
        assertThat(n.getTitulo()).isEqualTo("Venció el plazo de «Ecología general»");
        assertThat(n.getContenido()).contains("16/11/2026").contains("renueva el curso");
        verify(correoService).avisoCurso(eq("e@t.co"), eq("Esteban"), eq("Venció el plazo de «Ecología general»"), contains("Conservas tu avance"));
    }

    @Test @DisplayName("Repaso terminado → cerrada; conserva certificado y notas")
    void cierra() {
        Inscripcion c = insc(2, "completada"); c.setRepasoHasta(ahora.minusHours(2));
        when(inscripcionRepository.findByEstadoAndRepasoHastaBefore("completada", ahora)).thenReturn(List.of(c));
        RevisionPlazosResponse r = servicio.revisar(ahora);
        assertThat(r.getCerradas()).isEqualTo(1);
        assertThat(c.getEstado()).isEqualTo("cerrada");
        assertThat(notificaciones().get(0).getContenido()).contains("Conservas tu certificado").contains("16/11/2026");
    }

    @Test @DisplayName("Avisos de 15 días: una sola vez por plazo, con los días que faltan")
    void avisos() {
        Inscripcion a = insc(3, "activa"); a.setAccesoHasta(ahora.plusDays(9));
        Inscripcion b = insc(4, "completada"); b.setRepasoHasta(ahora.plusDays(15));
        when(inscripcionRepository.findByEstadoAndAccesoHastaBetweenAndAvisoVencimientoEnIsNull("activa", ahora, ahora.plusDays(15))).thenReturn(List.of(a));
        when(inscripcionRepository.findByEstadoAndRepasoHastaBetweenAndAvisoRepasoEnIsNull("completada", ahora, ahora.plusDays(15))).thenReturn(List.of(b));
        RevisionPlazosResponse r = servicio.revisar(ahora);
        assertThat(r.getAvisosVencimiento()).isEqualTo(1);
        assertThat(r.getAvisosRepaso()).isEqualTo(1);
        assertThat(a.getAvisoVencimientoEn()).isEqualTo(ahora);
        assertThat(b.getAvisoRepasoEn()).isEqualTo(ahora);
        assertThat(a.getEstado()).isEqualTo("activa");
        assertThat(notificaciones()).extracting(Notificacion::getTitulo)
                .containsExactly("Te quedan 9 días para terminar «Ecología general»", "Te quedan 15 días de repaso en «Ecología general»");
        verify(correoService, times(2)).avisoCurso(anyString(), anyString(), anyString(), anyString());
    }

    @Test @DisplayName("Sin plazos por revisar no cambia nada ni avisa; días que faltan nunca negativos")
    void sinCambios() {
        RevisionPlazosResponse r = servicio.revisar(ahora);
        assertThat(r.getVencidas() + r.getCerradas() + r.getAvisosVencimiento() + r.getAvisosRepaso()).isZero();
        assertThat(r.getRevisadoEn()).isEqualTo(ahora);
        verifyNoInteractions(notificacionRepository, correoService);
        assertThat(AccesoCursoServiceImpl.dias(ahora, ahora.minusDays(3))).isZero();
        assertThat(AccesoCursoServiceImpl.dias(ahora, ahora.plusDays(30))).isEqualTo(30);
    }
}
