package co.biolearn.service.impl;

import co.biolearn.dto.PerfilEstudianteResponse;
import co.biolearn.dto.UnirseInstitucionRequest;
import co.biolearn.enums.EstadoEstudiante;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.InstitucionRepository;
import co.biolearn.repository.NotificacionRepository;
import co.biolearn.repository.PerfilEstudianteRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AfiliacionEstudianteServiceImpl — «Unirme a mi colegio»")
class AfiliacionEstudianteServiceImplTest {

    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private InstitucionRepository institucionRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @InjectMocks private AfiliacionEstudianteServiceImpl servicio;

    private Usuario u;
    private PerfilEstudiante est;
    private Institucion colegio;

    @BeforeEach
    void datos() {
        u = Usuario.builder().id(30).nombre("Ana").apellido("Ruiz").correo("ana@t.co").tipoUsuario(RolUsuario.estudiante).estado(EstadoUsuario.activo).build();
        est = PerfilEstudiante.builder().id(9).usuario(u).estado(EstadoEstudiante.activo)
                .grado(Grado.builder().id(1).nombre("Noveno").build()).area(AreaAcademica.builder().id(1).nombre("Biología").build()).build();
        colegio = Institucion.builder().id(4).nombre("Colegio San Rafael").codigoAfiliacion("ABCD2345").estado("activa").cuposContratados(10).build();
        when(perfilEstudianteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(est));
        when(institucionRepository.findByCodigoAfiliacionIgnoreCase("abcd2345")).thenReturn(Optional.of(colegio));
        when(institucionRepository.findByCodigoAfiliacionIgnoreCase("ABCD2345")).thenReturn(Optional.of(colegio));
        when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(3L);
    }

    private PerfilEstudianteResponse unirse(String codigo) { return servicio.unirse(new UnirseInstitucionRequest(codigo), u); }

    @Test
    @DisplayName("El estudiante independiente se une con el código (sin importar mayúsculas) y se le notifica")
    void seUne() {
        PerfilEstudianteResponse r = unirse(" abcd2345 ");
        assertThat(r.getIdInstitucion()).isEqualTo(4);
        assertThat(r.getInstitucion()).isEqualTo("Colegio San Rafael");
        assertThat(r.getGrado()).isEqualTo("Noveno");
        assertThat(est.getInstitucion()).isSameAs(colegio);
        verify(perfilEstudianteRepository).save(est);
        ArgumentCaptor<Notificacion> n = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(n.capture());
        assertThat(n.getValue().getTitulo()).isEqualTo("Te uniste a Colegio San Rafael");
        assertThat(n.getValue().getTipo()).isEqualTo("sistema");
    }

    @Test
    @DisplayName("Cambiar de colegio con el código del nuevo; con el código del mismo colegio responde 409")
    void cambiarDeColegio() {
        est.setInstitucion(Institucion.builder().id(5).nombre("Colegio B").build());
        PerfilEstudianteResponse r = unirse("ABCD2345");
        assertThat(r.getInstitucion()).isEqualTo("Colegio San Rafael");
        ArgumentCaptor<Notificacion> n = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(n.capture());
        assertThat(n.getValue().getTitulo()).isEqualTo("Cambiaste a Colegio San Rafael");
        assertThat(n.getValue().getContenido()).contains("Ya no perteneces a Colegio B").contains("Conservas tus cursos");
        assertThatThrownBy(() -> unirse("ABCD2345")).isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("Ya perteneces a «Colegio San Rafael»");
    }

    @Test
    @DisplayName("Salir del colegio deja al estudiante independiente; si ya lo es responde 409")
    void salir() {
        est.setInstitucion(colegio);
        PerfilEstudianteResponse r = servicio.salir(u);
        assertThat(r.getIdInstitucion()).isNull();
        assertThat(est.getInstitucion()).isNull();
        verify(perfilEstudianteRepository).save(est);
        verify(notificacionRepository).save(argThat(n -> n.getTitulo().equals("Saliste de Colegio San Rafael")));
        assertThatThrownBy(() -> servicio.salir(u)).isInstanceOf(SolicitudInvalidaException.class).hasMessage("No perteneces a ningún colegio");
    }

    @Test
    @DisplayName("El Administrador asigna, cambia y quita la institución; valida cupos y que sea un estudiante")
    void administrador() {
        Usuario admin = Usuario.builder().id(1).tipoUsuario(RolUsuario.admin).build();
        when(institucionRepository.findById(4)).thenReturn(Optional.of(colegio));
        assertThat(servicio.asignarPorAdministrador(30, 4, admin).getInstitucion()).isEqualTo("Colegio San Rafael");
        verify(notificacionRepository).save(argThat(n -> n.getTitulo().equals("Ahora perteneces a Colegio San Rafael") && n.getEmisor() == admin));
        assertThatThrownBy(() -> servicio.asignarPorAdministrador(30, 4, admin)).hasMessage("El estudiante ya pertenece a esa institución");
        assertThat(servicio.asignarPorAdministrador(30, null, admin).getIdInstitucion()).isNull();
        assertThatThrownBy(() -> servicio.asignarPorAdministrador(30, null, admin)).hasMessage("El estudiante ya es independiente");
        assertThatThrownBy(() -> servicio.asignarPorAdministrador(30, 99, admin)).isInstanceOf(RecursoNoEncontradoException.class);
        when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(10L);
        assertThatThrownBy(() -> servicio.asignarPorAdministrador(30, 4, admin)).hasMessageContaining("no tiene cupos");
        assertThatThrownBy(() -> servicio.asignarPorAdministrador(77, 4, admin)).hasMessage("Solo se puede cambiar así la institución de un estudiante");
    }

    @Test
    @DisplayName("Código inexistente, institución inactiva o sin cupos: 409 con el mismo mensaje del registro")
    void reglasDelCodigo() {
        assertThatThrownBy(() -> unirse("ZZZZZZZZ")).isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El código de institución no es válido. Verifícalo con tu colegio.");
        colegio.setEstado("inactiva");
        assertThatThrownBy(() -> unirse("ABCD2345")).hasMessage("Esta institución no está activa en este momento");
        colegio.setEstado("activa");
        when(perfilEstudianteRepository.countByInstitucion_IdAndEstado(4, EstadoEstudiante.activo)).thenReturn(10L);
        assertThatThrownBy(() -> unirse("ABCD2345")).hasMessageContaining("no tiene cupos disponibles");
        colegio.setCuposContratados(null);                 // sin límite de cupos
        assertThat(unirse("ABCD2345").getIdInstitucion()).isEqualTo(4);
    }

    @Test
    @DisplayName("Solo los estudiantes usan esta función (403)")
    void soloEstudiantes() {
        Usuario docente = Usuario.builder().id(20).tipoUsuario(RolUsuario.docente).build();
        assertThatThrownBy(() -> servicio.unirse(new UnirseInstitucionRequest("ABCD2345"), docente)).isInstanceOf(SinPermisoException.class);
    }
}
