package co.biolearn.repository;

import co.biolearn.model.Curso;
import co.biolearn.model.Inscripcion;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integración con MySQL (biolearn_test) del acceso a los cursos por tiempo: migración 011 (meses del curso, estados
 * vencida y cerrada, fechas del plazo y del repaso y consultas de la revisión diaria). Se omite si biolearn_test no
 * está disponible. Cada prueba deshace sus datos al terminar.
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("baseDisponible")
@DisplayName("Integración MySQL — acceso a los cursos por tiempo (biolearn_test, migración 011)")
class AccesoIntegracionTest {

    static boolean baseDisponible() { return InscripcionIntegracionTest.baseDisponible(); }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry r) { r.add("spring.datasource.url", InscripcionIntegracionTest::urlPrueba); }

    @Autowired private DataSource dataSource;
    @Autowired private InscripcionRepository inscripcionRepository;
    @Autowired private CursoRepository cursoRepository;
    private JdbcTemplate db;
    private int idCurso, idEst;

    private int insertar(String sql, Object... args) {
        db.update(sql, args);
        return db.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    @BeforeEach
    void datosBase() {
        db = new JdbcTemplate(dataSource);
        long m = System.nanoTime();
        int idArea = insertar("INSERT INTO area_academica (nombre) VALUES (?)", "Área AC " + m);
        int idGrado = insertar("INSERT INTO grado (nombre, nivel, orden) VALUES (?, 'secundaria', 9)", "Grado AC " + m);
        int uEst = insertar("INSERT INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado) VALUES ('Est','AC',?,?,'x','estudiante','activo')", "ac" + m, "ac" + m + "@it.co");
        idEst = insertar("INSERT INTO perfil_estudiante (id_usuario, id_grado, id_area) VALUES (?,?,?)", uEst, idGrado, idArea);
        idCurso = insertar("INSERT INTO curso (id_grado, id_area, nombre, descripcion, objetivos, estado) VALUES (?,?,?,'d','o','activo')", idGrado, idArea, "Curso AC " + m);
    }

    @Test @DisplayName("Un curso nuevo trae 12 meses para terminar y 6 de repaso; se guardan otros valores")
    void mesesDelCurso() {
        Curso c = cursoRepository.findById(idCurso).orElseThrow();
        assertThat(c.getMesesParaTerminar()).isEqualTo(12);
        assertThat(c.getMesesRepaso()).isEqualTo(6);
        c.setMesesParaTerminar(24); c.setMesesRepaso(0);
        cursoRepository.saveAndFlush(c);
        assertThat(db.queryForObject("SELECT CONCAT(meses_para_terminar, '-', meses_repaso) FROM curso WHERE id_curso = ?", String.class, idCurso)).isEqualTo("24-0");
    }

    @Test @DisplayName("Los estados vencida y cerrada existen con sus fechas; un estado inventado se rechaza")
    void estadosNuevos() {
        int v = insertar("INSERT INTO inscripcion (id_curso, id_estudiante, estado, acceso_desde, acceso_hasta) VALUES (?,?,'vencida', NOW() - INTERVAL 13 MONTH, NOW() - INTERVAL 1 MONTH)", idCurso, idEst);
        Inscripcion i = inscripcionRepository.findById(v).orElseThrow();
        assertThat(i.getEstado()).isEqualTo("vencida");
        assertThat(i.getAccesoHasta()).isBefore(LocalDateTime.now());
        db.update("UPDATE inscripcion SET estado = 'cerrada', fecha_completacion = NOW() - INTERVAL 7 MONTH, repaso_hasta = NOW() - INTERVAL 1 MONTH WHERE id_inscripcion = ?", v);
        assertThat(db.queryForObject("SELECT estado FROM inscripcion WHERE id_inscripcion = ?", String.class, v)).isEqualTo("cerrada");
        assertThatThrownBy(() -> db.update("UPDATE inscripcion SET estado = 'caducada' WHERE id_inscripcion = ?", v)).isInstanceOf(DataAccessException.class);
    }

    @Test @DisplayName("Consultas de la revisión diaria: plazos vencidos, repasos terminados y avisos pendientes de 15 días")
    void consultasDeLaRevision() {
        int vencida = insertar("INSERT INTO inscripcion (id_curso, id_estudiante, estado, acceso_desde, acceso_hasta) VALUES (?,?,'activa', NOW() - INTERVAL 12 MONTH, NOW() - INTERVAL 1 DAY)", idCurso, idEst);
        LocalDateTime ahora = LocalDateTime.now();
        assertThat(ids(inscripcionRepository.findByEstadoAndAccesoHastaBefore("activa", ahora))).contains(vencida);
        db.update("UPDATE inscripcion SET acceso_hasta = NOW() + INTERVAL 10 DAY WHERE id_inscripcion = ?", vencida);
        assertThat(ids(inscripcionRepository.findByEstadoAndAccesoHastaBetweenAndAvisoVencimientoEnIsNull("activa", ahora, ahora.plusDays(15)))).contains(vencida);
        db.update("UPDATE inscripcion SET aviso_vencimiento_en = NOW() WHERE id_inscripcion = ?", vencida);
        assertThat(ids(inscripcionRepository.findByEstadoAndAccesoHastaBetweenAndAvisoVencimientoEnIsNull("activa", ahora, ahora.plusDays(15)))).doesNotContain(vencida);
        db.update("UPDATE inscripcion SET estado = 'completada', fecha_completacion = NOW() - INTERVAL 6 MONTH, repaso_hasta = NOW() - INTERVAL 1 HOUR WHERE id_inscripcion = ?", vencida);
        assertThat(ids(inscripcionRepository.findByEstadoAndRepasoHastaBefore("completada", LocalDateTime.now()))).contains(vencida);
    }

    private static List<Integer> ids(List<Inscripcion> l) { return l.stream().map(Inscripcion::getId).toList(); }
}
