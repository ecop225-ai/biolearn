package co.biolearn.repository;

import co.biolearn.model.ConsecutivoNotaCredito;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integración con MySQL (biolearn_test) del Módulo de Pagos, Fase 2: migración 008 (reembolso, reembolso_inscripcion,
 * nota_credito, consecutivo_nota_credito y el estado en_reembolso de la inscripción). Se omite si biolearn_test no
 * está disponible. Cada prueba deshace sus datos al terminar.
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("baseDisponible")
@DisplayName("Integración MySQL — reembolsos y notas crédito (biolearn_test, migración 008)")
class ReembolsoIntegracionTest {

    static boolean baseDisponible() { return InscripcionIntegracionTest.baseDisponible(); }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry r) { r.add("spring.datasource.url", InscripcionIntegracionTest::urlPrueba); }

    @Autowired private DataSource dataSource;
    @Autowired private ReembolsoInscripcionRepository reembolsoInscripcionRepository;
    @Autowired private ConsecutivoNotaCreditoRepository consecutivoRepository;
    @Autowired private ReembolsoRepository reembolsoRepository;
    private JdbcTemplate db;
    private int idUsuario, idEst, idInsc, idPago, idLinea, idFactura;
    private long marca;

    private int insertar(String sql, Object... args) {
        db.update(sql, args);
        return db.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    @BeforeEach
    void datosBase() {
        db = new JdbcTemplate(dataSource);
        marca = System.nanoTime();
        int area = insertar("INSERT INTO area_academica (nombre) VALUES (?)", "Área RE " + marca);
        int grado = insertar("INSERT INTO grado (nombre, nivel, orden) VALUES (?, 'secundaria', 9)", "Grado RE " + marca);
        idUsuario = insertar("INSERT INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado) VALUES ('Est','RE',?,?,'x','estudiante','activo')", "re" + marca, "re" + marca + "@it.co");
        idEst = insertar("INSERT INTO perfil_estudiante (id_usuario, id_grado, id_area) VALUES (?,?,?)", idUsuario, grado, area);
        int curso = insertar("INSERT INTO curso (id_grado, id_area, nombre, descripcion, objetivos, estado, precio) VALUES (?,?,?,'d','o','activo', 95000)", grado, area, "Curso RE " + marca);
        idInsc = insertar("INSERT INTO inscripcion (id_curso, id_estudiante, estado) VALUES (?,?, 'activa')", curso, idEst);
        idPago = insertar("INSERT INTO pago (id_estudiante, medio_pago, monto, estado, fecha_pago) VALUES (?, 'wompi_tarjeta', 95000, 'aprobado', NOW())", idEst);
        idLinea = insertar("INSERT INTO pago_inscripcion (id_pago, id_inscripcion, monto_asignado) VALUES (?,?, 95000)", idPago, idInsc);
        idFactura = insertar("INSERT INTO factura (id_pago, id_estudiante, numero_factura, monto_total) VALUES (?,?,?, 95000)", idPago, idEst, "BIO-FAC-RE-" + marca);
    }

    private int reembolso(String estado) {
        return insertar("INSERT INTO reembolso (id_pago, id_solicitante, motivo, monto, estado) VALUES (?,?, 'Motivo de prueba', 95000, ?)", idPago, idUsuario, estado);
    }

    private int linea(int idReembolso) {
        return insertar("INSERT INTO reembolso_inscripcion (id_reembolso, id_pago_inscripcion, monto, avance, estado_inscripcion_previo) VALUES (?,?, 95000, 5, 'activa')", idReembolso, idLinea);
    }

    @Test @DisplayName("Estados del reembolso y de la inscripción de la migración 008; los inventados se rechazan")
    void estados() {
        for (String e : List.of("solicitado", "aprobado", "devuelto", "rechazado", "cancelado"))
            assertThatCode(() -> reembolso(e)).doesNotThrowAnyException();
        assertThatThrownBy(() -> reembolso("pagado")).isInstanceOf(DataAccessException.class);
        assertThatCode(() -> db.update("UPDATE inscripcion SET estado = 'en_reembolso' WHERE id_inscripcion = ?", idInsc)).doesNotThrowAnyException();
        assertThatThrownBy(() -> db.update("UPDATE inscripcion SET estado = 'pausada' WHERE id_inscripcion = ?", idInsc)).isInstanceOf(DataAccessException.class);
    }

    @Test @DisplayName("Datos de la devolución: medio cuenta_bancaria con tipo ahorros; un medio o tipo inventado se rechaza")
    void datosDevolucion() {
        int id = insertar("INSERT INTO reembolso (id_pago, id_solicitante, motivo, monto, medio_devolucion, banco, tipo_cuenta, numero_cuenta, titular, documento_titular) " +
                "VALUES (?,?, 'Motivo', 95000, 'cuenta_bancaria', 'Bancolombia', 'ahorros', '1234567890', 'Titular', '1001234567')", idPago, idUsuario);
        assertThat(reembolsoRepository.findById(id)).get().extracting(r -> r.getNumeroCuenta()).isEqualTo("1234567890");
        assertThat(db.queryForObject("SELECT medio_devolucion FROM reembolso WHERE id_reembolso = ?", String.class, reembolso("solicitado"))).isEqualTo("mismo_medio");
        assertThatThrownBy(() -> insertar("INSERT INTO reembolso (id_pago, id_solicitante, motivo, monto, medio_devolucion) VALUES (?,?, 'M', 1, 'efectivo')", idPago, idUsuario))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> insertar("INSERT INTO reembolso (id_pago, id_solicitante, motivo, monto, tipo_cuenta) VALUES (?,?, 'M', 1, 'nomina')", idPago, idUsuario))
                .isInstanceOf(DataAccessException.class);
    }

    @Test @DisplayName("Un curso pagado aparece una sola vez en la misma solicitud (uq_reembolso_linea) y se borra en cascada")
    void lineaUnicaYCascada() {
        int r = reembolso("solicitado");
        linea(r);
        assertThatThrownBy(() -> linea(r)).isInstanceOf(DataIntegrityViolationException.class);
        db.update("DELETE FROM reembolso WHERE id_reembolso = ?", r);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM reembolso_inscripcion WHERE id_reembolso = ?", Integer.class, r)).isZero();
    }

    @Test @DisplayName("La consulta «curso en una solicitud vigente» solo cuenta los estados indicados")
    void enSolicitudVigente() {
        int r = reembolso("solicitado");
        linea(r);
        assertThat(reembolsoInscripcionRepository.existsByPagoInscripcion_IdAndReembolso_EstadoIn(idLinea, List.of("solicitado"))).isTrue();
        db.update("UPDATE reembolso SET estado = 'rechazado' WHERE id_reembolso = ?", r);
        assertThat(reembolsoInscripcionRepository.existsByPagoInscripcion_IdAndReembolso_EstadoIn(idLinea, List.of("solicitado", "aprobado", "devuelto"))).isFalse();
    }

    @Test @DisplayName("Nota crédito: número único, una por reembolso y exige una factura existente")
    void notaCredito() {
        int r1 = reembolso("aprobado"), r2 = reembolso("aprobado");
        String n = "BIO-NC-IT-" + marca;
        String sql = "INSERT INTO nota_credito (id_reembolso, id_factura, id_estudiante, numero_nota, motivo, monto_total) VALUES (?,?,?,?, 'M', 95000)";
        insertar(sql, r1, idFactura, idEst, n);
        assertThatThrownBy(() -> insertar(sql, r2, idFactura, idEst, n)).isInstanceOf(DataIntegrityViolationException.class);         // uq_nota_numero
        assertThatThrownBy(() -> insertar(sql, r1, idFactura, idEst, n + "-2")).isInstanceOf(DataIntegrityViolationException.class);   // uq_nota_reembolso
        assertThatThrownBy(() -> insertar(sql, r2, 999999999, idEst, n + "-3")).isInstanceOf(DataIntegrityViolationException.class);   // fk_nc_factura
    }

    @Test @DisplayName("El consecutivo de notas crédito se lee con bloqueo y avanza (consecutivo_nota_credito)")
    void consecutivo() {
        int anio = 1900 + (int) (marca % 99);
        db.update("DELETE FROM consecutivo_nota_credito WHERE anio = ?", anio);
        assertThat(consecutivoRepository.bloquear(anio)).isEmpty();
        consecutivoRepository.saveAndFlush(ConsecutivoNotaCredito.builder().anio(anio).ultimo(1).build());
        ConsecutivoNotaCredito c = consecutivoRepository.bloquear(anio).orElseThrow();
        c.setUltimo(c.getUltimo() + 1);
        consecutivoRepository.saveAndFlush(c);
        assertThat(db.queryForObject("SELECT ultimo FROM consecutivo_nota_credito WHERE anio = ?", Integer.class, anio)).isEqualTo(2);
    }
}
