package co.biolearn.repository;

import co.biolearn.model.DatosFacturacion;
import co.biolearn.model.DatosRecaudo;
import co.biolearn.model.ReporteFinanciero;
import co.biolearn.model.Usuario;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integración con MySQL (biolearn_test) del Módulo de Pagos, Fase 3: migración 010 (columnas nuevas de
 * datos_facturacion, datos_recaudo y reporte_financiero). Se omite si biolearn_test no está disponible.
 * Cada prueba deshace sus datos al terminar.
 */
@Tag("integracion")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf("baseDisponible")
@DisplayName("Integración MySQL — datos de facturación, recaudo y reportes (biolearn_test, migración 010)")
class FinanzasIntegracionTest {

    static boolean baseDisponible() { return InscripcionIntegracionTest.baseDisponible(); }

    @DynamicPropertySource
    static void baseDePrueba(DynamicPropertyRegistry r) { r.add("spring.datasource.url", InscripcionIntegracionTest::urlPrueba); }

    @Autowired private DataSource dataSource;
    @Autowired private DatosFacturacionRepository datosFacturacionRepository;
    @Autowired private DatosRecaudoRepository datosRecaudoRepository;
    @Autowired private ReporteFinancieroRepository reporteRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    private JdbcTemplate db;
    private Usuario contador;

    @BeforeEach
    void datosBase() {
        db = new JdbcTemplate(dataSource);
        long marca = System.nanoTime();
        db.update("INSERT INTO usuario (nombre, apellido, nombre_usuario, correo, contrasena, tipo_usuario, estado) VALUES ('Cont','FI',?,?,'x','contador','activo')",
                "fi" + marca, "fi" + marca + "@it.co");
        contador = usuarioRepository.findById(db.queryForObject("SELECT LAST_INSERT_ID()", Integer.class)).orElseThrow();
    }

    @Test @DisplayName("Una versión nueva de los datos de facturación queda vigente con su autor y sus columnas nuevas")
    void versionFacturacion() {
        DatosFacturacion nueva = datosFacturacionRepository.saveAndFlush(DatosFacturacion.builder().razonSocial("BioLearn IT S.A.S.").nit("901999999-1")
                .direccion("Calle 1 # 2-3").ciudad("Cali").correo("it@biolearn.co").telefono("602 111 2233").actualizadoPor(contador).build());
        DatosFacturacion vigente = datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc().orElseThrow();
        assertThat(vigente.getId()).isEqualTo(nueva.getId());
        assertThat(db.queryForObject("SELECT id_usuario_actualizo FROM datos_facturacion WHERE id_datos_facturacion = ?", Integer.class, nueva.getId()))
                .isEqualTo(contador.getId());
        assertThat(datosFacturacionRepository.findAllByOrderByActualizadoEnDescIdDesc()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test @DisplayName("Datos de recaudo: la versión nueva queda vigente; un tipo de cuenta inventado se rechaza")
    void recaudo() {
        DatosRecaudo nueva = datosRecaudoRepository.saveAndFlush(DatosRecaudo.builder().banco("Davivienda").tipoCuenta("corriente")
                .numeroCuenta("4567891234").titular("BioLearn S.A.S.").documentoTitular("901000000-0").telefonoNequi("3001112233").actualizadoPor(contador).build());
        assertThat(datosRecaudoRepository.findFirstByOrderByActualizadoEnDescIdDesc()).get().extracting(DatosRecaudo::getId).isEqualTo(nueva.getId());
        assertThatThrownBy(() -> db.update("INSERT INTO datos_recaudo (banco, tipo_cuenta, numero_cuenta, titular, documento_titular) VALUES ('B','nomina','1','T','1')"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test @DisplayName("Registro de reportes: tipos y formatos de la migración; los inventados y el usuario inexistente se rechazan")
    void reportes() {
        for (String tipo : List.of("ingresos", "facturas", "reembolsos", "general"))
            assertThatCode(() -> reporteRepository.saveAndFlush(ReporteFinanciero.builder().tipo(tipo).desde(LocalDate.of(2026, 11, 1))
                    .hasta(LocalDate.of(2026, 11, 16)).formato("excel").motivo("Prueba de integración").usuario(contador).build())).doesNotThrowAnyException();
        assertThat(reporteRepository.findTop50ByOrderByGeneradoEnDescIdDesc()).first().extracting(ReporteFinanciero::getGeneradoEn)
                .satisfies(f -> assertThat(f).isBefore(LocalDateTime.now().plusMinutes(1)));
        assertThatThrownBy(() -> db.update("INSERT INTO reporte_financiero (tipo, desde, hasta, formato, motivo, id_usuario) VALUES ('ventas', CURDATE(), CURDATE(), 'pdf', 'M', ?)", contador.getId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update("INSERT INTO reporte_financiero (tipo, desde, hasta, formato, motivo, id_usuario) VALUES ('general', CURDATE(), CURDATE(), 'csv', 'M', ?)", contador.getId()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> db.update("INSERT INTO reporte_financiero (tipo, desde, hasta, formato, motivo, id_usuario) VALUES ('general', CURDATE(), CURDATE(), 'pdf', 'M', 999999999)"))
                .isInstanceOf(DataAccessException.class);
    }
}
