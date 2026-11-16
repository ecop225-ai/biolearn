package co.biolearn.service.impl;

import co.biolearn.dto.HistorialReporteResponse;
import co.biolearn.dto.ReporteFinancieroResponse;
import co.biolearn.dto.ReporteRequest;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolUsuario;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReporteFinancieroServiceImpl — reportes financieros en PDF y Excel (Pagos, Fase 3)")
class ReporteFinancieroServiceImplTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private NotaCreditoRepository notaCreditoRepository;
    @Mock private DatosFacturacionRepository datosFacturacionRepository;
    @Mock private ReporteFinancieroRepository reporteRepository;

    @InjectMocks private ReporteFinancieroServiceImpl servicio;

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1), HASTA = LocalDate.of(2026, 11, 16);
    private Usuario contador;
    private PerfilEstudiante esteban;
    private Pago tarjeta, transferencia;
    private Factura f1, f2;
    private Reembolso devuelto, porRevisar;
    private NotaCredito nota;

    private static Usuario usuario(int id, String nombre, RolUsuario rol) {
        return Usuario.builder().id(id).nombre(nombre).apellido("Prueba").correo(id + "@t.co").tipoUsuario(rol).estado(EstadoUsuario.activo).build();
    }

    private Pago pago(int id, String medio, String estado, LocalDateTime fecha, String... cursosYMontos) {
        Pago p = Pago.builder().id(id).estudiante(esteban).medioPago(medio).estado(estado).fechaPago(fecha).creadoEn(fecha).build();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cursosYMontos.length; i += 2) {
            Curso c = Curso.builder().id(id * 10 + i).nombre(cursosYMontos[i]).build();
            Inscripcion ins = Inscripcion.builder().id(id * 10 + i).curso(c).estudiante(esteban).build();
            BigDecimal m = new BigDecimal(cursosYMontos[i + 1]);
            p.getItems().add(PagoInscripcion.builder().id(id * 10 + i).pago(p).inscripcion(ins).montoAsignado(m).build());
            total = total.add(m);
        }
        p.setMonto(total);
        return p;
    }

    private static ReporteRequest req(String tipo) {
        return ReporteRequest.builder().tipo(tipo).desde(DESDE).hasta(HASTA).motivo("Cierre mensual de noviembre").build();
    }

    private static Map<String, Object> indicadores(ReporteFinancieroResponse r) {
        Map<String, Object> m = new LinkedHashMap<>();
        r.getIndicadores().forEach(i -> m.put(i.getEtiqueta(), i.getValor()));
        return m;
    }

    private static ReporteFinancieroResponse.Tabla tabla(ReporteFinancieroResponse r, String titulo) {
        return r.getTablas().stream().filter(t -> t.getTitulo().equals(titulo)).findFirst().orElseThrow();
    }

    @BeforeEach
    void datos() {
        contador = usuario(40, "Jairo", RolUsuario.contador);
        esteban = PerfilEstudiante.builder().id(9).usuario(usuario(30, "Esteban", RolUsuario.estudiante)).build();
        tarjeta = pago(1, "wompi_tarjeta", "aprobado", LocalDateTime.of(2026, 10, 20, 10, 0), "Ecología", "100000");
        transferencia = pago(2, "transferencia", "reembolsado", LocalDateTime.of(2026, 11, 5, 9, 0), "Genética", "90000", "Botánica", "60000");
        when(pagoRepository.findByEstadoInAndFechaPagoBetweenOrderByFechaPagoAsc(anyCollection(), any(), any())).thenReturn(List.of(tarjeta, transferencia));
        Pago cancelado = pago(3, "pse", "cancelado", LocalDateTime.of(2026, 11, 6, 9, 0), "Ecología", "100000");
        Pago pendiente = pago(4, "transferencia", "pendiente", LocalDateTime.of(2026, 11, 7, 9, 0), "Zoología", "70000");
        when(pagoRepository.findAll()).thenReturn(List.of(tarjeta, transferencia, cancelado, pendiente));

        f1 = Factura.builder().id(1).pago(tarjeta).estudiante(esteban).numero("BIO-FAC-2026-000001").descripcion("Inscripción a: Ecología")
                .montoTotal(new BigDecimal("100000")).fechaEmision(tarjeta.getFechaPago()).build();
        f2 = Factura.builder().id(2).pago(transferencia).estudiante(esteban).numero("BIO-FAC-2026-000002").descripcion("Inscripción a: Genética, Botánica")
                .montoTotal(new BigDecimal("150000")).fechaEmision(transferencia.getFechaPago()).build();
        when(facturaRepository.findByPago_Id(1)).thenReturn(Optional.of(f1));
        when(facturaRepository.findByPago_Id(2)).thenReturn(Optional.of(f2));
        when(facturaRepository.findByFechaEmisionBetweenOrderByFechaEmisionAsc(any(), any())).thenReturn(List.of(f1, f2));

        devuelto = Reembolso.builder().id(16).pago(transferencia).solicitante(esteban.getUsuario()).estado("devuelto")
                .monto(new BigDecimal("60000")).medioDevolucion("cuenta_bancaria").banco("Bancolombia").tipoCuenta("ahorros")
                .numeroCuenta("4561237890").aprobacionAutomatica(true).referenciaDevolucion("TRF-1")
                .solicitadoEn(LocalDateTime.of(2026, 11, 6, 8, 0)).devueltoEn(LocalDateTime.of(2026, 11, 7, 8, 0)).build();
        devuelto.getItems().add(ReembolsoInscripcion.builder().reembolso(devuelto).pagoInscripcion(transferencia.getItems().get(1)).monto(new BigDecimal("60000")).build());
        porRevisar = Reembolso.builder().id(17).pago(tarjeta).solicitante(esteban.getUsuario()).estado("solicitado")
                .monto(new BigDecimal("100000")).medioDevolucion("mismo_medio").solicitadoEn(LocalDateTime.of(2026, 10, 22, 8, 0)).build();
        porRevisar.getItems().add(ReembolsoInscripcion.builder().reembolso(porRevisar).pagoInscripcion(tarjeta.getItems().get(0)).monto(new BigDecimal("100000")).build());
        when(reembolsoRepository.findByPago_Id(1)).thenReturn(List.of(porRevisar));
        when(reembolsoRepository.findByPago_Id(2)).thenReturn(List.of(devuelto));
        when(reembolsoRepository.findBySolicitadoEnBetweenOrderBySolicitadoEnAsc(any(), any())).thenReturn(List.of(porRevisar, devuelto));
        when(reembolsoRepository.findByEstadoAndDevueltoEnBetween(eq("devuelto"), any(), any())).thenReturn(List.of(devuelto));

        nota = NotaCredito.builder().id(5).numero("BIO-NC-2026-000005").factura(f2).reembolso(devuelto).montoTotal(new BigDecimal("60000"))
                .fechaEmision(LocalDateTime.of(2026, 11, 6, 9, 0)).build();
        when(notaCreditoRepository.findByFactura_IdOrderByFechaEmisionAsc(2)).thenReturn(List.of(nota));
        when(notaCreditoRepository.findByFactura_IdOrderByFechaEmisionAsc(1)).thenReturn(List.of());
        when(notaCreditoRepository.findByReembolso_Id(16)).thenReturn(Optional.of(nota));
        when(notaCreditoRepository.findAll()).thenReturn(List.of(nota));
        when(datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc()).thenReturn(Optional.of(
                DatosFacturacion.builder().id(1).razonSocial("BioLearn S.A.S.").nit("901000000-0").build()));
        when(reporteRepository.save(any(ReporteFinanciero.class))).thenAnswer(a -> {
            ReporteFinanciero r = a.getArgument(0); r.setId(77); r.setGeneradoEn(LocalDateTime.of(2026, 11, 16, 18, 0)); return r;
        });
    }

    @Test @DisplayName("Período inválido: inicio posterior al fin o más de un año → 400")
    void periodo() {
        assertThatThrownBy(() -> servicio.generar(ReporteRequest.builder().tipo("ingresos").desde(HASTA).hasta(DESDE).motivo("Prueba").build(), contador))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("La fecha inicial no puede ser posterior a la final");
        assertThatThrownBy(() -> servicio.generar(ReporteRequest.builder().tipo("ingresos").desde(LocalDate.of(2025, 1, 1)).hasta(HASTA).motivo("Prueba").build(), contador))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("un año");
        verify(reporteRepository, never()).save(any());
    }

    @Test @DisplayName("Ingresos: brutos, reembolsado, netos, por medio, por curso y detalle; queda registrado como PDF con el motivo")
    void ingresos() {
        ReporteFinancieroResponse r = servicio.generar(req("ingresos"), contador);
        Map<String, Object> ind = indicadores(r);
        assertThat((BigDecimal) ind.get("Ingresos brutos")).isEqualByComparingTo("250000");
        assertThat((BigDecimal) ind.get("Reembolsado (notas crédito)")).isEqualByComparingTo("60000");
        assertThat((BigDecimal) ind.get("Ingresos netos")).isEqualByComparingTo("190000");
        assertThat(ind.get("Pagos aprobados")).isEqualTo(2L);
        assertThat((BigDecimal) ind.get("Valor promedio por pago")).isEqualByComparingTo("125000");
        assertThat(tabla(r, "Por medio de pago").getFilas()).extracting(f -> f.get(0)).containsExactly("Tarjeta", "Transferencia");
        assertThat((BigDecimal) tabla(r, "Por medio de pago").getFilas().get(1).get(3)).isEqualByComparingTo("0.6");
        assertThat(tabla(r, "Por curso").getFilas()).extracting(f -> f.get(0)).containsExactly("Ecología", "Genética", "Botánica");
        List<Object> detalle = tabla(r, "Pagos aprobados").getFilas().get(1);
        assertThat(detalle).contains("#2", "BIO-FAC-2026-000002", "Esteban Prueba", "Genética, Botánica", "Transferencia");
        assertThat((BigDecimal) detalle.get(9)).isEqualByComparingTo("90000");
        assertThat(r.getRazonSocial()).isEqualTo("BioLearn S.A.S.");
        assertThat(r.getGeneradoPor()).isEqualTo("Jairo Prueba");
        verify(reporteRepository).save(argThat(x -> "ingresos".equals(x.getTipo()) && "pdf".equals(x.getFormato())
                && "Cierre mensual de noviembre".equals(x.getMotivo()) && x.getUsuario() == contador));
    }

    @Test @DisplayName("Facturas: notas crédito asociadas, saldo y estado de cada factura")
    void facturas() {
        ReporteFinancieroResponse r = servicio.generar(req("facturas"), contador);
        Map<String, Object> ind = indicadores(r);
        assertThat(ind.get("Facturas emitidas")).isEqualTo(2L);
        assertThat((BigDecimal) ind.get("Total facturado")).isEqualByComparingTo("250000");
        assertThat(ind.get("Facturas con nota crédito")).isEqualTo(1L);
        assertThat((BigDecimal) ind.get("Saldo facturado")).isEqualByComparingTo("190000");
        List<Object> f = tabla(r, "Facturas emitidas").getFilas().get(1);
        assertThat(f).contains("BIO-FAC-2026-000002", "BIO-NC-2026-000005", "Corregida en parte");
        assertThat(tabla(r, "Facturas emitidas").getFilas().get(0)).contains("Vigente");
    }

    @Test @DisplayName("Reembolsos: por estado, aprobación automática y destino siempre enmascarado")
    void reembolsos() {
        ReporteFinancieroResponse r = servicio.generar(req("reembolsos"), contador);
        Map<String, Object> ind = indicadores(r);
        assertThat(ind.get("Solicitudes")).isEqualTo(2L);
        assertThat((BigDecimal) ind.get("Devuelto")).isEqualByComparingTo("60000");
        assertThat(ind.get("Aprobadas automáticamente")).isEqualTo(1L);
        assertThat(tabla(r, "Por estado").getFilas()).extracting(f -> f.get(0), f -> f.get(1))
                .contains(tuple("En revisión", 1L), tuple("Devuelto", 1L), tuple("Rechazado", 0L));
        List<List<Object>> detalle = tabla(r, "Solicitudes de reembolso").getFilas();
        assertThat(detalle.get(0)).contains("Mismo medio (Tarjeta)", "En revisión");
        assertThat(detalle.get(1)).contains("Cuenta de ahorros ••••7890 · Bancolombia", "Automática", "BIO-NC-2026-000005", "TRF-1");
        assertThat(detalle.get(1).toString()).doesNotContain("4561237890");
    }

    @Test @DisplayName("General: indicadores, mes a mes, pagos por estado y cursos con más ingresos")
    void general() {
        ReporteFinancieroResponse r = servicio.generar(req("general"), contador);
        Map<String, Object> ind = indicadores(r);
        assertThat((BigDecimal) ind.get("Ingresos netos")).isEqualByComparingTo("190000");
        assertThat(ind.get("Notas crédito emitidas")).isEqualTo(1L);
        assertThat(ind.get("Transferencias por confirmar")).isEqualTo(1L);
        assertThat(tabla(r, "Mes a mes").getFilas()).extracting(f -> f.get(0)).containsExactly("Octubre 2026", "Noviembre 2026");
        assertThat((BigDecimal) tabla(r, "Mes a mes").getFilas().get(1).get(4)).isEqualByComparingTo("90000");
        assertThat(tabla(r, "Pagos creados en el período por estado").getTotal().get(1)).isEqualTo(4L);
        assertThat(tabla(r, "Cursos con más ingresos").getTotal()).isNull();
    }

    @Test @DisplayName("Excel: hoja Resumen con indicadores y tablas, una hoja por detalle; registrado como excel")
    void excel() throws Exception {
        byte[] xlsx = servicio.excel(req("ingresos"), contador);
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) partes.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThat(partes.get("xl/workbook.xml")).contains("name=\"Resumen\"").contains("name=\"Pagos aprobados\"");
        assertThat(partes.get("xl/worksheets/sheet1.xml")).contains("Reporte de ingresos — BioLearn S.A.S.").contains("Ingresos netos").contains("Por medio de pago");
        assertThat(partes.get("xl/worksheets/sheet2.xml")).contains("BIO-FAC-2026-000002");
        verify(reporteRepository).save(argThat(x -> "excel".equals(x.getFormato())));
    }

    @Test @DisplayName("Historial con el nombre del tipo y el autor")
    void historial() {
        when(reporteRepository.findTop50ByOrderByGeneradoEnDescIdDesc()).thenReturn(List.of(ReporteFinanciero.builder().id(1).tipo("general")
                .desde(DESDE).hasta(HASTA).formato("excel").motivo("Junta directiva").usuario(contador).generadoEn(LocalDateTime.now()).build()));
        HistorialReporteResponse h = servicio.historial().get(0);
        assertThat(h.getTipoNombre()).isEqualTo("Reporte financiero general");
        assertThat(h.getGeneradoPor()).isEqualTo("Jairo Prueba");
    }
}
