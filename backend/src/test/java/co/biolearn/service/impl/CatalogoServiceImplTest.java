package co.biolearn.service.impl;

import co.biolearn.dto.AreaRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CatalogoServiceImpl — grados y áreas académicas")
class CatalogoServiceImplTest {

    @Mock private GradoRepository gradoRepository;
    @Mock private AreaAcademicaRepository areaRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;

    @InjectMocks private CatalogoServiceImpl catalogoService;

    @BeforeEach
    void configurarComunes() {
        when(gradoRepository.save(any(Grado.class))).thenAnswer(inv -> inv.getArgument(0));
        when(areaRepository.save(any(AreaAcademica.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Grado grado(int id, String nombre, String estado) {
        return Grado.builder().id(id).nombre(nombre).nivel("secundaria").orden(id + 5).estado(estado).build();
    }

    private GradoRequest gradoReq(String nombre, String nivel, Integer orden) {
        GradoRequest r = new GradoRequest();
        r.setNombre(nombre);
        r.setNivel(nivel);
        r.setOrden(orden);
        return r;
    }

    private CambiarEstadoRequest estado(String valor) {
        CambiarEstadoRequest r = new CambiarEstadoRequest();
        r.setEstado(valor);
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Grados")
    class Grados {

        @Test
        @DisplayName("Listado público → solo grados activos, en orden")
        void listadoPublico() {
            when(gradoRepository.findByEstadoOrderByOrdenAsc("activo")).thenReturn(List.of(grado(1, "Sexto", "activo")));

            List<GradoResponse> r = catalogoService.listarGrados(true);

            assertThat(r).extracting(GradoResponse::getNombre).containsExactly("Sexto");
            verify(gradoRepository, never()).findAllByOrderByOrdenAsc();
        }

        @Test
        @DisplayName("Listado del Administrador → incluye inactivos")
        void listadoAdmin() {
            when(gradoRepository.findAllByOrderByOrdenAsc())
                    .thenReturn(List.of(grado(1, "Sexto", "activo"), grado(2, "Séptimo", "inactivo")));

            assertThat(catalogoService.listarGrados(false)).hasSize(2);
        }

        @Test
        @DisplayName("Crear grado válido → activo, con nivel en minúsculas")
        void crearValido() {
            GradoResponse r = catalogoService.crearGrado(gradoReq(" Quinto ", "Primaria", 5));

            assertThat(r.getNombre()).isEqualTo("Quinto");
            assertThat(r.getNivel()).isEqualTo("primaria");
            assertThat(r.getEstado()).isEqualTo("activo");
        }

        @Test
        @DisplayName("Crear grado con nombre repetido → 409")
        void nombreRepetido() {
            when(gradoRepository.existsByNombreIgnoreCase("Sexto")).thenReturn(true);

            assertThatThrownBy(() -> catalogoService.crearGrado(gradoReq("Sexto", "secundaria", 6)))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ya existe un grado con ese nombre");
        }

        @Test
        @DisplayName("Crear grado con nivel inexistente → IllegalArgumentException (400)")
        void nivelInvalido() {
            assertThatThrownBy(() -> catalogoService.crearGrado(gradoReq("Quinto", "universitaria", 5)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("primaria, secundaria o media");
        }

        @Test
        @DisplayName("Crear grado sin orden → IllegalArgumentException (400)")
        void sinOrden() {
            assertThatThrownBy(() -> catalogoService.crearGrado(gradoReq("Quinto", "primaria", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El orden del grado es obligatorio");
        }

        @Test
        @DisplayName("Editar grado → solo cambian los campos enviados")
        void editar() {
            Grado sexto = grado(1, "Sexto", "activo");
            when(gradoRepository.findById(1)).thenReturn(Optional.of(sexto));
            GradoRequest r = new GradoRequest();
            r.setDescripcion("Primer año de secundaria");

            catalogoService.actualizarGrado(1, r);

            assertThat(sexto.getNombre()).isEqualTo("Sexto");
            assertThat(sexto.getDescripcion()).isEqualTo("Primer año de secundaria");
        }

        @Test
        @DisplayName("Renombrar grado con el nombre de otro → 409")
        void renombrarRepetido() {
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado(1, "Sexto", "activo")));
            when(gradoRepository.existsByNombreIgnoreCaseAndIdNot("Séptimo", 1)).thenReturn(true);

            assertThatThrownBy(() -> catalogoService.actualizarGrado(1, gradoReq("Séptimo", null, null)))
                    .isInstanceOf(SolicitudInvalidaException.class);
        }

        @Test
        @DisplayName("Inactivar grado → estado inactivo")
        void inactivar() {
            Grado sexto = grado(1, "Sexto", "activo");
            when(gradoRepository.findById(1)).thenReturn(Optional.of(sexto));

            catalogoService.cambiarEstadoGrado(1, estado("Inactivo"));

            assertThat(sexto.getEstado()).isEqualTo("inactivo");
        }

        @Test
        @DisplayName("Detalle de un grado → incluye los estudiantes que lo tienen asignado")
        void detalleGrado() {
            when(gradoRepository.findById(1)).thenReturn(Optional.of(grado(1, "Sexto", "activo")));
            when(perfilEstudianteRepository.countByGrado_Id(1)).thenReturn(12L);

            GradoResponse r = catalogoService.obtenerGrado(1);

            assertThat(r.getNombre()).isEqualTo("Sexto");
            assertThat(r.getEstudiantesAsociados()).isEqualTo(12);
        }

        @Test
        @DisplayName("Grado inexistente → 404")
        void inexistente() {
            when(gradoRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogoService.cambiarEstadoGrado(99, estado("activo")))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El grado indicado no existe");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Áreas académicas")
    class Areas {

        @Test
        @DisplayName("Crear área válida → activa")
        void crearValida() {
            AreaRequest r = new AreaRequest();
            r.setNombre(" Botánica ");

            assertThat(catalogoService.crearArea(r).getEstado()).isEqualTo("activo");
        }

        @Test
        @DisplayName("Crear área sin nombre → IllegalArgumentException (400)")
        void sinNombre() {
            assertThatThrownBy(() -> catalogoService.crearArea(new AreaRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Crear área con nombre repetido → 409")
        void nombreRepetido() {
            when(areaRepository.existsByNombreIgnoreCase("Bioinformática")).thenReturn(true);
            AreaRequest r = new AreaRequest();
            r.setNombre("Bioinformática");

            assertThatThrownBy(() -> catalogoService.crearArea(r))
                    .isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessage("Ya existe un área académica con ese nombre");
        }

        @Test
        @DisplayName("Estado inexistente → IllegalArgumentException (400)")
        void estadoInvalido() {
            when(areaRepository.findById(1)).thenReturn(Optional.of(AreaAcademica.builder().id(1).nombre("Ecología").estado("activo").build()));

            assertThatThrownBy(() -> catalogoService.cambiarEstadoArea(1, estado("archivado")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("activo o inactivo");
        }

        @Test
        @DisplayName("Detalle de un área → incluye los estudiantes que la tienen asignada")
        void detalleArea() {
            when(areaRepository.findById(2)).thenReturn(Optional.of(AreaAcademica.builder().id(2).nombre("Ecología").estado("activo").build()));
            when(perfilEstudianteRepository.countByArea_Id(2)).thenReturn(5L);

            assertThat(catalogoService.obtenerArea(2).getEstudiantesAsociados()).isEqualTo(5);
        }

        @Test
        @DisplayName("Detalle de un área inexistente → 404")
        void detalleAreaInexistente() {
            when(areaRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogoService.obtenerArea(99))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El área académica indicada no existe");
        }

        @Test
        @DisplayName("Área inexistente → 404")
        void inexistente() {
            when(areaRepository.findById(99)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> catalogoService.actualizarArea(99, new AreaRequest()))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }
}
