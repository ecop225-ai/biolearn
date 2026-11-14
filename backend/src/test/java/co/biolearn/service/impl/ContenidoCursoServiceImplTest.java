package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfilAmplio;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContenidoCursoServiceImpl — contenido de los cursos (Fase 2)")
class ContenidoCursoServiceImplTest {

    @Mock private CursoRepository cursoRepository;
    @Mock private CursoDocenteRepository cursoDocenteRepository;
    @Mock private LeccionRepository leccionRepository;
    @Mock private MaterialContenidoRepository materialRepository;
    @Mock private RecursoRepository recursoRepository;
    @Mock private PerfilDocenteRepository perfilDocenteRepository;
    @Mock private AreaAcademicaRepository areaRepository;
    @Mock private GradoRepository gradoRepository;
    @Mock private ArticuloCientificoRepository articuloRepository;
    @Mock private RecursoApoyoRepository apoyoRepository;
    @Mock private co.biolearn.service.ArchivoFirma archivoFirma;
    @Mock private CursoServiceImpl cursoService;

    @InjectMocks private ContenidoCursoServiceImpl servicio;

    private Usuario titularU, colaboradorU, invitadoU, ajenoU, adminU;
    private PerfilDocente titular, colaborador, invitado, ajeno;
    private Curso curso;

    private static Usuario usuario(int id, RolUsuario rol) {
        return Usuario.builder().id(id).nombre("Nombre" + id).apellido("Apellido").correo(id + "@biolearn.co").tipoUsuario(rol).estado(EstadoUsuario.activo).build();
    }

    private PerfilDocente perfil(Usuario u, int idPerfil) {
        PerfilDocente p = PerfilDocente.builder().id(idPerfil).usuario(u).estado(EstadoPerfilAmplio.activo).build();
        when(perfilDocenteRepository.findByUsuario_Id(u.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private void asignar(PerfilDocente d, String rol) {
        when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, d.getId()))
                .thenReturn(Optional.of(CursoDocente.builder().curso(curso).docente(d).rol(rol).estado("activo").build()));
    }

    @BeforeEach
    void datos() {
        curso = Curso.builder().id(10).nombre("Genética básica").estado("borrador").precio(BigDecimal.ZERO)
                .area(AreaAcademica.builder().id(1).nombre("Genética").estado("activo").build())
                .grado(Grado.builder().id(1).nombre("Noveno").nivel("secundaria").orden(9).estado("activo").build()).build();
        titularU = usuario(20, RolUsuario.docente); colaboradorU = usuario(21, RolUsuario.docente);
        invitadoU = usuario(22, RolUsuario.docente); ajenoU = usuario(23, RolUsuario.docente); adminU = usuario(1, RolUsuario.admin);
        titular = perfil(titularU, 5); colaborador = perfil(colaboradorU, 6); invitado = perfil(invitadoU, 7); ajeno = perfil(ajenoU, 8);
        curso.setDocenteTitular(titular);
        asignar(titular, "titular"); asignar(colaborador, "colaborador"); asignar(invitado, "invitado");
        when(cursoDocenteRepository.findByCurso_IdAndDocente_Id(10, 8)).thenReturn(Optional.empty());
        when(cursoRepository.findById(10)).thenReturn(Optional.of(curso));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(i -> { Curso c = i.getArgument(0); if (c.getId() == null) c.setId(10); return c; });
        when(leccionRepository.save(any(Leccion.class))).thenAnswer(i -> { Leccion l = i.getArgument(0); if (l.getId() == null) l.setId(100); return l; });
        when(cursoService.pendientesParaPublicar(any())).thenReturn(List.of());
        when(archivoFirma.firmar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Leccion leccion(int id, int orden, String estado) {
        Leccion l = Leccion.builder().id(id).curso(curso).nombre("Lección " + id).orden(orden).tipo("teoria").estado(estado).build();
        when(leccionRepository.findById(id)).thenReturn(Optional.of(l));
        return l;
    }

    private MaterialContenido material(int id, Leccion l, String estado) {
        MaterialContenido m = MaterialContenido.builder().id(id).leccion(l).docente(titular).nombre("Material " + id).tipo("lectura").orden(1).estado(estado).build();
        when(materialRepository.findById(id)).thenReturn(Optional.of(m));
        return m;
    }

    private static CambiarEstadoRequest estado(String e) { CambiarEstadoRequest r = new CambiarEstadoRequest(); r.setEstado(e); return r; }

    // ── cursos del docente ──
    @Nested @DisplayName("Cursos del docente")
    class CursosDocente {

        @Test @DisplayName("Proponer un curso → borrador, precio 0 y el docente como titular")
        void proponer() {
            when(areaRepository.findById(1)).thenReturn(Optional.of(curso.getArea()));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(curso.getGrado()));
            CursoDocenteResponse r = servicio.proponerCurso(CursoDocenteRequest.builder().nombre("Ecología").idArea(1).idGrado(1).build(), titularU);

            ArgumentCaptor<Curso> c = ArgumentCaptor.forClass(Curso.class);
            verify(cursoRepository).save(c.capture());
            assertThat(c.getValue().getEstado()).isEqualTo("borrador");
            assertThat(c.getValue().getPrecio()).isEqualByComparingTo("0");
            assertThat(c.getValue().getDocenteTitular()).isEqualTo(titular);
            assertThat(c.getValue().getDescripcion()).isEqualTo(""); // columna NOT NULL
            assertThat(r.getMiRol()).isEqualTo("titular");
            verify(cursoDocenteRepository).save(argThat(a -> "titular".equals(a.getRol())));
        }

        @Test @DisplayName("Proponer sin nombre, área o grado → 400")
        void proponerIncompleto() {
            assertThatThrownBy(() -> servicio.proponerCurso(CursoDocenteRequest.builder().nombre("X").build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("obligatorios");
        }

        @Test @DisplayName("Proponer con un nombre repetido → 409")
        void proponerRepetido() {
            when(cursoRepository.existsByNombreIgnoreCaseAndEstadoNot("Ecología", "archivado")).thenReturn(true);
            assertThatThrownBy(() -> servicio.proponerCurso(CursoDocenteRequest.builder().nombre("Ecología").idArea(1).idGrado(1).build(), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Ya existe un curso con ese nombre");
        }

        @Test @DisplayName("Un usuario sin perfil docente no puede proponer → 403")
        void proponerSinPerfil() {
            when(perfilDocenteRepository.findByUsuario_Id(1)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.proponerCurso(CursoDocenteRequest.builder().nombre("X").idArea(1).idGrado(1).build(), adminU))
                    .isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("El titular edita los datos académicos")
        void editarTitular() {
            CursoDocenteResponse r = servicio.actualizarCursoDocente(10, CursoDocenteRequest.builder().descripcion("Nueva").build(), titularU);
            assertThat(r.getDescripcion()).isEqualTo("Nueva");
        }

        @Test @DisplayName("Un colaborador no edita los datos del curso → 403")
        void editarColaborador() {
            assertThatThrownBy(() -> servicio.actualizarCursoDocente(10, CursoDocenteRequest.builder().descripcion("X").build(), colaboradorU))
                    .isInstanceOf(SinPermisoException.class).hasMessageContaining("Solo el docente titular");
        }

        @Test @DisplayName("Mis cursos → con el rol del docente en cada curso")
        void misCursos() {
            when(cursoDocenteRepository.findByDocente_IdAndEstado(6, "activo"))
                    .thenReturn(List.of(CursoDocente.builder().curso(curso).docente(colaborador).rol("colaborador").estado("activo").build()));
            List<CursoDocenteResponse> r = servicio.misCursos(colaboradorU);
            assertThat(r).hasSize(1);
            assertThat(r.get(0).getMiRol()).isEqualTo("colaborador");
            assertThat(r.get(0).isPuedeEditarContenido()).isTrue();
        }
    }

    // ── lecciones ──
    @Nested @DisplayName("Lecciones")
    class Lecciones {

        @Test @DisplayName("Crear una lección → borrador, al final del orden")
        void crear() {
            when(leccionRepository.maxOrden(10)).thenReturn(3);
            LeccionResponse r = servicio.crearLeccion(10, LeccionRequest.builder().nombre("Mendel").build(), colaboradorU);
            assertThat(r.getEstado()).isEqualTo("borrador");
            assertThat(r.getOrden()).isEqualTo(4);
            assertThat(r.getTipo()).isEqualTo("teoria");
        }

        @Test @DisplayName("Crear sin nombre → 400; tipo inválido → 400")
        void crearInvalida() {
            assertThatThrownBy(() -> servicio.crearLeccion(10, new LeccionRequest(), titularU)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.crearLeccion(10, LeccionRequest.builder().nombre("X").tipo("taller").build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("teoria, practica, evaluacion o mixta");
        }

        @Test @DisplayName("Un invitado no puede crear lecciones → 403")
        void invitadoNoCrea() {
            assertThatThrownBy(() -> servicio.crearLeccion(10, LeccionRequest.builder().nombre("X").build(), invitadoU))
                    .isInstanceOf(SinPermisoException.class).hasMessage("Los docentes invitados solo pueden consultar el contenido");
        }

        @Test @DisplayName("Un docente ajeno al curso no puede ni consultar → 403")
        void ajenoNoConsulta() {
            assertThatThrownBy(() -> servicio.listarLecciones(10, ajenoU)).isInstanceOf(SinPermisoException.class).hasMessage("No eres docente de este curso");
        }

        @Test @DisplayName("El Administrador consulta, pero no edita → 403 al editar")
        void adminSoloConsulta() {
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(List.of());
            assertThatCode(() -> servicio.listarLecciones(10, adminU)).doesNotThrowAnyException();
            assertThatThrownBy(() -> servicio.crearLeccion(10, LeccionRequest.builder().nombre("X").build(), adminU))
                    .isInstanceOf(SinPermisoException.class).hasMessageContaining("solo los docentes del curso lo editan");
        }

        @Test @DisplayName("Un invitado sí puede consultar")
        void invitadoConsulta() {
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(List.of());
            assertThatCode(() -> servicio.listarLecciones(10, invitadoU)).doesNotThrowAnyException();
        }

        @Test @DisplayName("Curso archivado → no se modifica (409)")
        void cursoArchivado() {
            curso.setEstado("archivado");
            assertThatThrownBy(() -> servicio.crearLeccion(10, LeccionRequest.builder().nombre("X").build(), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("archivado");
        }

        @Test @DisplayName("Activar sin material activo → 409")
        void activarSinMaterial() {
            leccion(1, 1, "borrador");
            assertThatThrownBy(() -> servicio.cambiarEstadoLeccion(1, estado("activa"), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("al menos un material activo");
        }

        @Test @DisplayName("Activar con material activo → activa")
        void activar() {
            leccion(1, 1, "borrador");
            when(materialRepository.countByLeccion_IdAndEstado(1, "activo")).thenReturn(1L);
            assertThat(servicio.cambiarEstadoLeccion(1, estado("activa"), titularU).getEstado()).isEqualTo("activa");
        }

        @Test @DisplayName("Transición no permitida (borrador → inactiva) → 409")
        void transicionInvalida() {
            leccion(1, 1, "borrador");
            assertThatThrownBy(() -> servicio.cambiarEstadoLeccion(1, estado("inactiva"), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("solo se permite: activa, archivada");
        }

        @Test @DisplayName("Archivar una lección que es prerrequisito de una activa → 409")
        void archivarPrerequisito() {
            leccion(1, 1, "activa");
            when(leccionRepository.existsByPrerequisito_IdAndEstado(1, "activa")).thenReturn(true);
            assertThatThrownBy(() -> servicio.cambiarEstadoLeccion(1, estado("archivada"), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("prerrequisito de una lección activa");
        }

        @Test @DisplayName("Estado inexistente → 400")
        void estadoInvalido() {
            leccion(1, 1, "borrador");
            assertThatThrownBy(() -> servicio.cambiarEstadoLeccion(1, estado("publicada"), titularU)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("Lección inexistente → 404")
        void inexistente() {
            when(leccionRepository.findById(99)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.detalleLeccion(99, titularU)).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    // ── prerrequisitos y orden ──
    @Nested @DisplayName("Prerrequisitos y orden")
    class Orden {

        @Test @DisplayName("Prerrequisito válido: una lección anterior del mismo curso")
        void prerequisitoValido() {
            Leccion l1 = leccion(1, 1, "activa"); leccion(2, 2, "borrador");
            LeccionResponse r = servicio.actualizarLeccion(2, LeccionRequest.builder().idPrerequisito(1).build(), titularU);
            assertThat(r.getIdPrerequisito()).isEqualTo(1);
            assertThat(r.getPrerequisito()).isEqualTo(l1.getNombre());
        }

        @Test @DisplayName("Prerrequisito posterior en el orden → 400")
        void prerequisitoPosterior() {
            leccion(1, 1, "borrador"); leccion(2, 2, "borrador");
            assertThatThrownBy(() -> servicio.actualizarLeccion(1, LeccionRequest.builder().idPrerequisito(2).build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("anterior en el orden");
        }

        @Test @DisplayName("Una lección como su propio prerrequisito → 400")
        void prerequisitoPropio() {
            leccion(1, 1, "borrador");
            assertThatThrownBy(() -> servicio.actualizarLeccion(1, LeccionRequest.builder().idPrerequisito(1).build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("su propio prerrequisito");
        }

        @Test @DisplayName("Prerrequisito de otro curso → 400")
        void prerequisitoOtroCurso() {
            leccion(2, 2, "borrador");
            Curso otro = Curso.builder().id(11).nombre("Otro").estado("borrador").build();
            when(leccionRepository.findById(50)).thenReturn(Optional.of(Leccion.builder().id(50).curso(otro).nombre("Ajena").orden(1).estado("activa").build()));
            assertThatThrownBy(() -> servicio.actualizarLeccion(2, LeccionRequest.builder().idPrerequisito(50).build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("mismo curso");
        }

        @Test @DisplayName("idPrerequisito = 0 quita el prerrequisito")
        void quitarPrerequisito() {
            Leccion l1 = leccion(1, 1, "activa"); Leccion l2 = leccion(2, 2, "borrador"); l2.setPrerequisito(l1);
            assertThat(servicio.actualizarLeccion(2, LeccionRequest.builder().idPrerequisito(0).build(), titularU).getIdPrerequisito()).isNull();
        }

        @Test @DisplayName("Reordenar → asigna 1..n y deja las archivadas al final")
        void reordenar() {
            Leccion a = leccion(1, 1, "activa"), b = leccion(2, 2, "borrador"), c = leccion(3, 3, "archivada");
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(new ArrayList<>(List.of(a, b, c)));
            servicio.reordenarLecciones(10, new OrdenLeccionesRequest(List.of(2, 1)), titularU);
            assertThat(b.getOrden()).isEqualTo(1);
            assertThat(a.getOrden()).isEqualTo(2);
            assertThat(c.getOrden()).isEqualTo(3);
            verify(leccionRepository, times(2)).saveAllAndFlush(anyList()); // dos pasos por el orden único
        }

        @Test @DisplayName("Reordenar dejando una lección antes de su prerrequisito → 409")
        void reordenarRompePrerequisito() {
            Leccion a = leccion(1, 1, "activa"), b = leccion(2, 2, "borrador"); b.setPrerequisito(a);
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(new ArrayList<>(List.of(a, b)));
            assertThatThrownBy(() -> servicio.reordenarLecciones(10, new OrdenLeccionesRequest(List.of(2, 1)), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("antes de su prerrequisito");
        }

        @Test @DisplayName("Reordenar con una lista incompleta o repetida → 400")
        void reordenarIncompleto() {
            Leccion a = leccion(1, 1, "activa"), b = leccion(2, 2, "borrador");
            when(leccionRepository.findByCurso_IdOrderByOrdenAsc(10)).thenReturn(new ArrayList<>(List.of(a, b)));
            assertThatThrownBy(() -> servicio.reordenarLecciones(10, new OrdenLeccionesRequest(List.of(1, 1)), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("exactamente las 2 lecciones");
        }
    }

    // ── materiales ──
    @Nested @DisplayName("Materiales")
    class Materiales {

        @Test @DisplayName("Crear un material → activo, con el docente que lo creó")
        void crear() {
            leccion(1, 1, "borrador");
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("Leyes de Mendel").tipo("Lectura").build(), colaboradorU);
            verify(materialRepository).save(argThat(m -> "lectura".equals(m.getTipo()) && "activo".equals(m.getEstado()) && m.getDocente() == colaborador));
        }

        @Test @DisplayName("Tipo de material inválido → 400; lección archivada → 409")
        void crearInvalido() {
            leccion(1, 1, "borrador"); leccion(2, 2, "archivada");
            assertThatThrownBy(() -> servicio.crearMaterial(1, MaterialRequest.builder().nombre("X").tipo("podcast").build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.crearMaterial(2, MaterialRequest.builder().nombre("X").tipo("lectura").build(), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("archivada");
        }

        @Test @DisplayName("Inactivar el único material activo de una lección activa → 409")
        void inactivarUltimo() {
            Leccion l = leccion(1, 1, "activa"); material(5, l, "activo");
            when(materialRepository.countByLeccion_IdAndEstado(1, "activo")).thenReturn(1L);
            assertThatThrownBy(() -> servicio.cambiarEstadoMaterial(5, estado("inactivo"), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("único material activo");
        }

        @Test @DisplayName("Inactivar un material de una lección en borrador → permitido")
        void inactivarEnBorrador() {
            Leccion l = leccion(1, 1, "borrador"); MaterialContenido m = material(5, l, "activo");
            when(materialRepository.countByLeccion_IdAndEstado(1, "activo")).thenReturn(1L);
            servicio.cambiarEstadoMaterial(5, estado("inactivo"), titularU);
            assertThat(m.getEstado()).isEqualTo("inactivo");
        }
    }

    // ── datos propios de cada tipo (diseño original del prototipo) ──
    @Nested @DisplayName("Datos de cada tipo de material y del curso")
    class DatosDelTipo {

        @Test @DisplayName("Lectura → guarda el contenido del editor y el color de fondo")
        void lectura() {
            leccion(1, 1, "borrador");
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("Lectura").tipo("lectura")
                    .contenido("<h2>ADN</h2><p>Texto</p>").colorFondo("#FFF8E7").build(), titularU);
            verify(materialRepository, atLeastOnce()).save(argThat(m -> "<h2>ADN</h2><p>Texto</p>".equals(m.getContenido()) && "#FFF8E7".equals(m.getColorFondo())));
            verify(recursoRepository, never()).save(any());
        }

        @Test @DisplayName("Video → crea el recurso principal con la URL para embeber, la duración y guarda la guía")
        void video() {
            leccion(1, 1, "borrador");
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("Video").tipo("video").url("https://youtu.be/dQw4w9WgXcQ")
                    .duracionMin(12).guia("<ol><li>Observa</li></ol>").build(), titularU);
            verify(recursoRepository).save(argThat(r -> r.getUrl().equals("https://www.youtube.com/embed/dQw4w9WgXcQ") && r.getDuracionMin() == 12 && "video".equals(r.getTipoRecurso())));
            verify(materialRepository, atLeastOnce()).save(argThat(m -> "<ol><li>Observa</li></ol>".equals(m.getGuia())));
        }

        @Test @DisplayName("Laboratorio → recurso interactivo con el proveedor externo")
        void laboratorio() {
            leccion(1, 1, "borrador");
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("Lab").tipo("laboratorio").url("https://phet.colorado.edu/sim").proveedor("PhET").build(), titularU);
            verify(recursoRepository).save(argThat(r -> "interactivo".equals(r.getTipoRecurso()) && "PhET".equals(r.getDescripcion())));
        }

        @Test @DisplayName("Artículo sin autores → 400; con autores → se guarda en articulo_cientifico")
        void articulo() {
            leccion(1, 1, "borrador");
            assertThatThrownBy(() -> servicio.crearMaterial(1, MaterialRequest.builder().nombre("Art").tipo("articulo").build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Los autores del artículo son obligatorios");
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            when(articuloRepository.findFirstByMaterial_Id(9)).thenReturn(Optional.empty());
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("La doble hélice").tipo("articulo").autores("Watson, J.; Crick, F.")
                    .revista("Nature").doi("10.1038/171737a0").build(), titularU);
            verify(articuloRepository).save(argThat(a -> "La doble hélice".equals(a.getTitulo()) && "Nature".equals(a.getRevista())));
        }

        @Test @DisplayName("Fase 3: el HTML de la lectura se limpia (sin scripts ni eventos) y conserva el formato")
        void lecturaSanitizada() {
            leccion(1, 1, "borrador");
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("Lectura").tipo("lectura")
                    .contenido("<p style=\"color:red\" onclick=\"robar()\">Texto <b>clave</b></p><script>alert(1)</script>").build(), titularU);
            verify(materialRepository).save(argThat(m -> m.getContenido().contains("<b>clave</b>") && m.getContenido().contains("color:red")
                    && !m.getContenido().contains("script") && !m.getContenido().contains("onclick")));
        }

        @Test @DisplayName("Recurso principal del video → queda en recurso_apoyo como «primario»")
        void apoyoPrimario() {
            leccion(1, 1, "borrador");
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("Video").tipo("video").url("https://youtu.be/dQw4w9WgXcQ").build(), titularU);
            verify(apoyoRepository).save(argThat(a -> "primario".equals(a.getTipoContenido()) && "video".equals(a.getTipoRecurso())
                    && "https://www.youtube.com/embed/dQw4w9WgXcQ".equals(a.getUrl()) && a.getUsuario() == titularU));
        }

        @Test @DisplayName("Recurso extra → «complementario»; artículo → «referencia» en recurso_apoyo")
        void apoyoComplementarioYReferencia() {
            Leccion l = leccion(1, 1, "borrador"); material(5, l, "activo");
            servicio.crearRecurso(5, RecursoRequest.builder().nombre("Enlace de apoyo").url("https://es.wikipedia.org/wiki/Manglar").tipoRecurso("enlace").build(), titularU);
            verify(apoyoRepository).save(argThat(a -> "complementario".equals(a.getTipoContenido()) && "Enlace de apoyo".equals(a.getTitulo())));
            when(materialRepository.save(any(MaterialContenido.class))).thenAnswer(i -> { MaterialContenido m = i.getArgument(0); if (m.getId() == null) m.setId(9); return m; });
            when(articuloRepository.findFirstByMaterial_Id(9)).thenReturn(Optional.empty());
            servicio.crearMaterial(1, MaterialRequest.builder().nombre("La doble hélice").tipo("articulo").autores("Watson y Crick").build(), titularU);
            verify(apoyoRepository).save(argThat(a -> "referencia".equals(a.getTipoContenido()) && "La doble hélice".equals(a.getTitulo())));
        }

        @Test @DisplayName("Código de curso repetido → 409; el código se guarda en mayúsculas")
        void codigoCurso() {
            when(areaRepository.findById(1)).thenReturn(Optional.of(curso.getArea()));
            when(gradoRepository.findById(1)).thenReturn(Optional.of(curso.getGrado()));
            when(cursoRepository.existsByCodigoIgnoreCase("GEN-101")).thenReturn(true);
            assertThatThrownBy(() -> servicio.proponerCurso(CursoDocenteRequest.builder().nombre("A").idArea(1).idGrado(1).codigo("gen-101").build(), titularU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Ya existe un curso con el código GEN-101");
            CursoDocenteResponse r = servicio.proponerCurso(CursoDocenteRequest.builder().nombre("B").idArea(1).idGrado(1).codigo("eco-201")
                    .color("#2E7D32").infoEstudiante("<p>Requisitos</p>").build(), titularU);
            assertThat(r.getCodigo()).isEqualTo("ECO-201");
            assertThat(r.getColor()).isEqualTo("#2E7D32");
            assertThat(r.getInfoEstudiante()).isEqualTo("<p>Requisitos</p>");
        }
    }

    // ── recursos ──
    @Nested @DisplayName("Recursos y videos de YouTube")
    class Recursos {

        @Test @DisplayName("Las URL de YouTube (watch, youtu.be y shorts) se convierten al formato para embeber")
        void youtube() {
            assertThat(ContenidoCursoServiceImpl.normalizarUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10", "video")).isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
            assertThat(ContenidoCursoServiceImpl.normalizarUrl("https://youtu.be/dQw4w9WgXcQ", "video")).isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
            assertThat(ContenidoCursoServiceImpl.normalizarUrl("https://youtube.com/shorts/dQw4w9WgXcQ", "video")).isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
        }

        @Test @DisplayName("Un video que no es de YouTube → 400")
        void videoNoYoutube() {
            assertThatThrownBy(() -> ContenidoCursoServiceImpl.normalizarUrl("https://vimeo.com/123", "video"))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("YouTube");
        }

        @Test @DisplayName("Otros recursos: http(s) o un archivo subido; cualquier otra cosa → 400")
        void otrasUrl() {
            assertThat(ContenidoCursoServiceImpl.normalizarUrl("https://es.wikipedia.org/wiki/ADN", "enlace")).startsWith("https://");
            assertThat(ContenidoCursoServiceImpl.normalizarUrl("/api/archivos/123e4567-e89b-12d3-a456-426614174000.pdf", "documento")).endsWith(".pdf");
            assertThatThrownBy(() -> ContenidoCursoServiceImpl.normalizarUrl("ftp://servidor/archivo", "documento")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ContenidoCursoServiceImpl.normalizarUrl("/api/archivos/../../etc/passwd", "documento")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("Crear un recurso de video → se guarda con la URL para embeber")
        void crearRecurso() {
            Leccion l = leccion(1, 1, "borrador"); material(5, l, "activo");
            servicio.crearRecurso(5, RecursoRequest.builder().nombre("Video").url("https://youtu.be/dQw4w9WgXcQ").tipoRecurso("video").build(), titularU);
            verify(recursoRepository).save(argThat(r -> r.getUrl().equals("https://www.youtube.com/embed/dQw4w9WgXcQ")));
        }

        @Test @DisplayName("Recurso sin URL → 400; nivel inválido → 400")
        void recursoInvalido() {
            Leccion l = leccion(1, 1, "borrador"); material(5, l, "activo");
            assertThatThrownBy(() -> servicio.crearRecurso(5, RecursoRequest.builder().nombre("X").tipoRecurso("enlace").build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("obligatorios");
            assertThatThrownBy(() -> servicio.crearRecurso(5, RecursoRequest.builder().nombre("X").url("https://a.co").tipoRecurso("enlace").nivelAcademico("experto").build(), titularU))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("basico, intermedio o avanzado");
        }

        @Test @DisplayName("Un invitado no puede agregar recursos → 403")
        void invitadoNoAgrega() {
            Leccion l = leccion(1, 1, "borrador"); material(5, l, "activo");
            assertThatThrownBy(() -> servicio.crearRecurso(5, RecursoRequest.builder().nombre("X").url("https://a.co").tipoRecurso("enlace").build(), invitadoU))
                    .isInstanceOf(SinPermisoException.class);
        }
    }
}
