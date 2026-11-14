package co.biolearn.repository;

import co.biolearn.model.ConsecutivoFactura;
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
 * Integración con MySQL (biolearn_test) del Módulo de Pagos, Fase 1: migración 007 y restricciones de las tablas
 * pago, pago_inscripcion, factura y consecutivo_factura. Usa la misma conexión que InscripcionIntegracionTest y
 * se omite si biolearn_test no está disponible. Cada prueba deshace sus datos al terminar.
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("baseDisponible")
@DisplayName("Integración MySQL — pagos y facturas (biolearn_test, migración 007)")
class PagoIntegracionTest {

    static boolean baseDisponible() { return InscripcionIntegracionTest.baseDisponible(); }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry r) { r.add("spring.datasource.url", InscripcionIntegracionTest::urlPrueba); }

    @Autowired private DataSource dataSource;
    @Autowired private PagoInscripcionRepository pagoInscripcionRepository;
    @Autowired private ConsecutivoFacturaRepository consecutivoRepository;
    @Autowired private DatosFacturacionRepository datosFacturacionRepository;
    @Autowired private PagoRepository pagoRepository;
    private JdbcTemplate db;
    private int idEst, idInsc;
    private long marca;

    private int insertar(String sql, Object... args) {
        db.update(sql, args);
        return db.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    @BeforeEach
    void datosBase() {
        db = new JdbcTemplate(dataSource);
        marca = System.nanoTime();
        int area = insertar("INSERT INTO area_academica (nombre) VALUES (?)", "Área PG " + marca);
        int grado = insertar("INSERT INTO grado (nombre, nivel, orden) VALUES (?, 'secundaria', 9)", "Grado PG " + marca);
        int u = insertar("INSERT INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado) VALUES ('Est','PG',?,?,'x','estudiante','activo')", "pg" + marca, "pg" + marca + "@it.co");
        idEst = insertar("INSERT INTO perfil_estudiante (id_usuario, id_grado, id_area) VALUES (?,?,?)", u, grado, area);
        int curso = insertar("INSERT INTO curso (id_grado, id_area, nombre, descripcion, objetivos, estado, precio) VALUES (?,?,?,'d','o','activo', 95000)", grado, area, "Curso PG " + marca);
        idInsc = insertar("INSERT INTO inscripcion (id_curso, id_estudiante, estado) VALUES (?,?, 'pendiente_pago')", curso, idEst);
    }

    private int pago(String estado) {
        return insertar("INSERT INTO pago (id_estudiante, medio_pago, monto, estado) VALUES (?, 'transferencia', 95000, ?)", idEst, estado);
    }

    @Test @DisplayName("El estado «cancelado» existe y un estado inventado se rechaza (migración 007)")
    void estados() {
        assertThatCode(() -> pago("cancelado")).doesNotThrowAnyException();
        assertThatThrownBy(() -> pago("pagado")).isInstanceOf(DataAccessException.class);
    }

    @Test @DisplayName("El pago guarda el motivo del rechazo y el comprobante (migración 007)")
    void columnasNuevas() {
        int id = insertar("INSERT INTO pago (id_estudiante, medio_pago, monto, estado, motivo_rechazo, comprobante_url) VALUES (?, 'transferencia', 95000, 'rechazado', 'Valor distinto', '/api/archivos/x.pdf')", idEst);
        assertThat(db.queryForObject("SELECT motivo_rechazo FROM pago WHERE id_pago = ?", String.class, id)).isEqualTo("Valor distinto");
        assertThat(pagoRepository.findById(id)).get().extracting(p -> p.getComprobanteUrl()).isEqualTo("/api/archivos/x.pdf");
    }

    @Test @DisplayName("Una inscripción aparece una sola vez en el mismo pago (uq_pago_inscripcion)")
    void lineaUnica() {
        int p = pago("pendiente");
        insertar("INSERT INTO pago_inscripcion (id_pago, id_inscripcion, monto_asignado) VALUES (?,?, 95000)", p, idInsc);
        assertThatThrownBy(() -> insertar("INSERT INTO pago_inscripcion (id_pago, id_inscripcion, monto_asignado) VALUES (?,?, 95000)", p, idInsc))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test @DisplayName("La consulta «en un pago pendiente» solo cuenta los pagos pendientes")
    void enPagoPendiente() {
        int p = pago("pendiente");
        insertar("INSERT INTO pago_inscripcion (id_pago, id_inscripcion, monto_asignado) VALUES (?,?, 95000)", p, idInsc);
        assertThat(pagoInscripcionRepository.existsByInscripcion_IdAndPago_EstadoIn(idInsc, List.of("pendiente"))).isTrue();
        db.update("UPDATE pago SET estado = 'rechazado' WHERE id_pago = ?", p);
        assertThat(pagoInscripcionRepository.existsByInscripcion_IdAndPago_EstadoIn(idInsc, List.of("pendiente"))).isFalse();
    }

    @Test @DisplayName("El número de factura es único (uq_factura_numero)")
    void numeroFacturaUnico() {
        int p1 = pago("aprobado"), p2 = pago("aprobado");
        String n = "BIO-FAC-IT-" + marca;
        insertar("INSERT INTO factura (id_pago, id_estudiante, numero_factura, monto_total) VALUES (?,?,?, 95000)", p1, idEst, n);
        assertThatThrownBy(() -> insertar("INSERT INTO factura (id_pago, id_estudiante, numero_factura, monto_total) VALUES (?,?,?, 95000)", p2, idEst, n))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test @DisplayName("El consecutivo se lee con bloqueo y avanza (consecutivo_factura, migración 007)")
    void consecutivo() {
        int anio = 1900 + (int) (marca % 99);                              // un año que no usa la aplicación
        db.update("DELETE FROM consecutivo_factura WHERE anio = ?", anio);
        assertThat(consecutivoRepository.bloquear(anio)).isEmpty();
        consecutivoRepository.saveAndFlush(ConsecutivoFactura.builder().anio(anio).ultimo(1).build());
        ConsecutivoFactura c = consecutivoRepository.bloquear(anio).orElseThrow();
        c.setUltimo(c.getUltimo() + 1);
        consecutivoRepository.saveAndFlush(c);
        assertThat(db.queryForObject("SELECT ultimo FROM consecutivo_factura WHERE anio = ?", Integer.class, anio)).isEqualTo(2);
    }

    @Test @DisplayName("Los datos de facturación vigentes son los actualizados más recientemente")
    void datosVigentes() {
        insertar("INSERT INTO datos_facturacion (razon_social, nit, actualizado_en) VALUES ('Antigua IT', '1', '2000-01-01')");
        int nuevo = insertar("INSERT INTO datos_facturacion (razon_social, nit, actualizado_en) VALUES ('Vigente IT', '2', '2099-01-01')");
        assertThat(datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc()).get().extracting(d -> d.getId()).isEqualTo(nuevo);
    }

    @Test @DisplayName("Un pago exige un estudiante existente (fk_pago_estudiante)")
    void estudianteExistente() {
        assertThatThrownBy(() -> insertar("INSERT INTO pago (id_estudiante, medio_pago, monto) VALUES (999999999, 'pse', 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
