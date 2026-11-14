package co.biolearn.repository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

/**
 * Pruebas de integración contra MySQL real (recomendación de la Fase 1, tras DEF-005): verifican las restricciones
 * del esquema que una base simulada no detecta. Usan la base biolearn_test (solo estructura, ver sql/crear_biolearn_test.md),
 * con el usuario y la contraseña de application.properties. Cada prueba deshace sus datos al terminar.
 * Si biolearn_test no existe o MySQL no responde, la clase se omite.
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("baseDisponible")
@DisplayName("Integración MySQL — restricciones del esquema (biolearn_test)")
class InscripcionIntegracionTest {

    @Autowired private DataSource dataSource;
    @Autowired private ProgresoMaterialRepository progresoMaterialRepository;
    @Autowired private InscripcionRepository inscripcionRepository;
    private JdbcTemplate db;

    static Properties config() {
        Properties p = new Properties();
        try (InputStream in = InscripcionIntegracionTest.class.getResourceAsStream("/application.properties")) { p.load(in); } catch (Exception e) { return p; }
        return p;
    }

    static String urlPrueba() {
        return config().getProperty("spring.datasource.url", "").replace("/biolearn_db", "/biolearn_test");
    }

    static boolean baseDisponible() {
        Properties p = config();
        String usuario = p.getProperty("spring.datasource.username");
        try (Connection c = DriverManager.getConnection(urlPrueba(), usuario, p.getProperty("spring.datasource.password"))) {
            return c.isValid(2);
        } catch (Exception e) {
            // Se informa el motivo para no tener que adivinarlo (por ejemplo, un usuario sin permisos sobre biolearn_test)
            System.out.println("Pruebas de integración omitidas: no se pudo conectar a biolearn_test con el usuario "
                    + usuario + " (" + e.getMessage() + ")");
            return false;
        }
    }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry r) { r.add("spring.datasource.url", InscripcionIntegracionTest::urlPrueba); }

    private int idArea, idGrado, idInst, idCurso, idEst, idDoc, idLeccion;

    private int insertar(String sql, Object... args) {
        db.update(sql, args);
        return db.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    @BeforeEach
    void datosBase() {
        db = new JdbcTemplate(dataSource);
        long m = System.nanoTime();
        idArea = insertar("INSERT INTO area_academica (nombre) VALUES (?)", "Área IT " + m);
        idGrado = insertar("INSERT INTO grado (nombre, nivel, orden) VALUES (?, 'secundaria', 9)", "Grado IT " + m);
        idInst = insertar("INSERT INTO institucion (nombre, nit, codigo_afiliacion) VALUES (?, ?, ?)", "Colegio IT " + m, "NIT" + m % 100000000, ("C" + m).substring(0, 8));
        int uEst = insertar("INSERT INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado) VALUES ('Est','IT',?,?,'x','estudiante','activo')", "est" + m, "est" + m + "@it.co");
        idEst = insertar("INSERT INTO perfil_estudiante (id_usuario, id_institucion, id_grado, id_area) VALUES (?,?,?,?)", uEst, idInst, idGrado, idArea);
        int uDoc = insertar("INSERT INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado) VALUES ('Doc','IT',?,?,'x','docente','activo')", "doc" + m, "doc" + m + "@it.co");
        idDoc = insertar("INSERT INTO perfil_docente (id_usuario) VALUES (?)", uDoc);
        idCurso = insertar("INSERT INTO curso (id_grado, id_area, nombre, descripcion, objetivos, estado) VALUES (?,?,?,'d','o','activo')", idGrado, idArea, "Curso IT " + m);
        idLeccion = insertar("INSERT INTO leccion (id_curso, nombre, orden, estado) VALUES (?, 'L1', 1, 'activa')", idCurso);
    }

    private int inscripcion(String estado) {
        return insertar("INSERT INTO inscripcion (id_curso, id_estudiante, estado) VALUES (?,?,?)", idCurso, idEst, estado);
    }

    private int material(String estado) {
        return insertar("INSERT INTO material_contenido (id_leccion, id_docente, nombre, tipo, estado) VALUES (?,?, 'M', 'lectura', ?)", idLeccion, idDoc, estado);
    }

    @Test @DisplayName("Un estudiante no puede tener dos inscripciones en el mismo curso (uq_inscripcion)")
    void inscripcionUnica() {
        inscripcion("activa");
        assertThatThrownBy(() -> inscripcion("activa")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test @DisplayName("El estado pendiente_pago existe y un estado inventado se rechaza (migración 005)")
    void estados() {
        assertThatCode(() -> inscripcion("pendiente_pago")).doesNotThrowAnyException();
        db.update("DELETE FROM inscripcion WHERE id_curso = ?", idCurso);
        assertThatThrownBy(() -> inscripcion("pagada")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test @DisplayName("El código de inscripción es obligatorio y único (migración 005)")
    void codigoInscripcion() {
        insertar("INSERT INTO institucion_curso_contratado (id_institucion, id_curso, codigo_inscripcion) VALUES (?,?, 'ITCODE01')", idInst, idCurso);
        int otroCurso = insertar("INSERT INTO curso (id_grado, id_area, nombre, descripcion, objetivos) VALUES (?,?,?,'d','o')", idGrado, idArea, "Curso IT2 " + System.nanoTime());
        assertThatThrownBy(() -> insertar("INSERT INTO institucion_curso_contratado (id_institucion, id_curso, codigo_inscripcion) VALUES (?,?, 'ITCODE01')", idInst, otroCurso))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("INSERT INTO institucion_curso_contratado (id_institucion, id_curso) VALUES (?,?)", idInst, otroCurso))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test @DisplayName("Un material solo se marca una vez como visto (uq_progreso_material)")
    void progresoMaterialUnico() {
        int insc = inscripcion("activa"); int mat = material("activo");
        insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en, visto_en) VALUES (?,?, NOW(), NOW())", insc, mat);
        assertThatThrownBy(() -> insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en, visto_en) VALUES (?,?, NOW(), NOW())", insc, mat))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test @DisplayName("Borrar una inscripción borra en cascada su progreso")
    void cascada() {
        int insc = inscripcion("activa"); int mat = material("activo");
        insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en, visto_en) VALUES (?,?, NOW(), NOW())", insc, mat);
        insertar("INSERT INTO progreso_leccion (id_inscripcion, id_leccion) VALUES (?,?)", insc, idLeccion);
        db.update("DELETE FROM inscripcion WHERE id_inscripcion = ?", insc);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM progreso_material WHERE id_inscripcion = ?", Integer.class, insc)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM progreso_leccion WHERE id_inscripcion = ?", Integer.class, insc)).isZero();
    }

    @Test @DisplayName("La consulta de vistos cuenta solo los materiales activos")
    void contarVistosActivos() {
        int insc = inscripcion("activa");
        int activo = material("activo"); int inactivo = material("inactivo");
        insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en, visto_en) VALUES (?,?, NOW(), NOW())", insc, activo);
        insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en, visto_en) VALUES (?,?, NOW(), NOW())", insc, inactivo);
        assertThat(progresoMaterialRepository.contarVistosActivos(insc, idLeccion)).isEqualTo(1);
    }

    @Test @DisplayName("Un material abierto pero sin ver no cuenta en el avance (migración 006)")
    void abiertoSinVer() {
        int insc = inscripcion("activa");
        int visto = material("activo"); int abierto = material("activo");
        insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en, visto_en) VALUES (?,?, NOW(), NOW())", insc, visto);
        insertar("INSERT INTO progreso_material (id_inscripcion, id_material, abierto_en) VALUES (?,?, NOW())", insc, abierto);
        assertThat(progresoMaterialRepository.contarVistosActivos(insc, idLeccion)).isEqualTo(1);
        assertThat(progresoMaterialRepository.idsVistos(insc)).containsExactly(visto);
    }

    @Test @DisplayName("La fecha de apertura es obligatoria (migración 006)")
    void aperturaObligatoria() {
        int insc = inscripcion("activa"); int mat = material("activo");
        assertThatThrownBy(() -> insertar("INSERT INTO progreso_material (id_inscripcion, id_material) VALUES (?,?)", insc, mat))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test @DisplayName("Regresión de DEF-005: la descripción del curso no admite NULL")
    void cursoNotNull() {
        assertThatThrownBy(() -> insertar("INSERT INTO curso (id_grado, id_area, nombre, descripcion, objetivos) VALUES (?,?,?, NULL, 'o')", idGrado, idArea, "Curso NULL " + System.nanoTime()))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test @DisplayName("El repositorio encuentra la inscripción del estudiante en el curso")
    void repositorio() {
        int insc = inscripcion("activa");
        assertThat(inscripcionRepository.findByCurso_IdAndEstudiante_Id(idCurso, idEst)).get().extracting(i -> i.getId()).isEqualTo(insc);
    }
}
