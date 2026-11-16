package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfil;
import co.biolearn.enums.EstadoUsuario;
import co.biolearn.enums.RolContador;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.ArchivoFirma;
import co.biolearn.service.CorreoService;
import co.biolearn.service.PasarelaPago;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReembolsoServiceImpl — reembolsos con nota crédito (Pagos, Fase 2)")
class ReembolsoServiceImplTest {

    /** En los correos (HTML) los puntos del número enmascarado salen como entidades: •••• → &bull;&bull;&bull;&bull; */
    private static final String PUNTOS_HTML = "&bull;&bull;&bull;&bull;";


    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private ReembolsoInscripcionRepository reembolsoInscripcionRepository;
    @Mock private NotaCreditoRepository notaCreditoRepository;
    @Mock private ConsecutivoNotaCreditoRepository consecutivoRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private DatosFacturacionRepository datosFacturacionRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private PerfilContadorRepository perfilContadorRepository;
    @Mock private InscripcionServiceImpl inscripcionService;
    @Mock private PasarelaPago pasarela;
    @Mock private ArchivoFirma archivoFirma;
    @Mock private CorreoService correoService;

    @InjectMocks private ReembolsoServiceImpl servicio;

    private Usuario estU, otroU, contU, auditorU;
    private PerfilEstudiante est, otro;
    private PerfilContador contador;
    private final Map<Integer, Pago> pagos = new HashMap<>();
    private final Map<Integer, Reembolso> reembolsos = new HashMap<>();
    private final List<NotaCredito> notas = new ArrayList<>();
    private final Map<Integer, BigDecimal> avances = new HashMap<>();
    private int siguienteLinea = 900, siguienteReembolso = 50;

    private static Usuario usuario(int id, RolUsuario rol) {
        return Usuario.builder().id(id).nombre("N" + id).apellido("A").correo(id + "@t.co").tipoUsuario(rol).estado(EstadoUsuario.activo).build();
    }

    /** Pago aprobado con una línea por curso (inscripciones activas) y su factura. */
    private Pago pago(int id, String medio, LocalDateTime fechaPago, PerfilEstudiante e, int... idsInscripcion) {
        Pago p = Pago.builder().id(id).estudiante(e).medioPago(medio).estado("aprobado").fechaPago(fechaPago).referenciaExterna("PRUEBA-X").build();
        BigDecimal total = BigDecimal.ZERO;
        for (int ins : idsInscripcion) {
            Curso c = Curso.builder().id(ins).nombre("Curso " + ins).codigo("C-" + ins).precio(new BigDecimal("50000")).build();
            Inscripcion i = Inscripcion.builder().id(ins).curso(c).estudiante(e).estado("activa").build();
            p.getItems().add(PagoInscripcion.builder().id(siguienteLinea++).pago(p).inscripcion(i).montoAsignado(new BigDecimal("50000")).build());
            total = total.add(new BigDecimal("50000"));
        }
        p.setMonto(total);
        pagos.put(id, p);
        Factura f = Factura.builder().id(700 + id).pago(p).estudiante(e).numero("BIO-FAC-2026-0000" + id).montoTotal(total).build();
        when(facturaRepository.findByPago_Id(id)).thenReturn(Optional.of(f));
        return p;
    }

    private Inscripcion insc(Pago p, int idInscripcion) {
        return p.getItems().stream().map(PagoInscripcion::getInscripcion).filter(i -> i.getId() == idInscripcion).findFirst().orElseThrow();
    }

    @BeforeEach
    void datos() {
        estU = usuario(30, RolUsuario.estudiante); otroU = usuario(31, RolUsuario.estudiante);
        contU = usuario(40, RolUsuario.contador); auditorU = usuario(41, RolUsuario.contador);
        est = PerfilEstudiante.builder().id(9).usuario(estU).build();
        otro = PerfilEstudiante.builder().id(10).usuario(otroU).build();
        when(perfilEstudianteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(est));
        when(perfilEstudianteRepository.findByUsuario_Id(31)).thenReturn(Optional.of(otro));
        contador = PerfilContador.builder().id(3).usuario(contU).rolContador(RolContador.administrador_financiero).estado(EstadoPerfil.activo).build();
        when(perfilContadorRepository.findByUsuario_Id(40)).thenReturn(Optional.of(contador));
        when(perfilContadorRepository.findByUsuario_Id(41)).thenReturn(Optional.of(
                PerfilContador.builder().id(4).usuario(auditorU).rolContador(RolContador.auditor).estado(EstadoPerfil.activo).build()));
        when(pagoRepository.findById(anyInt())).thenAnswer(a -> Optional.ofNullable(pagos.get(a.<Integer>getArgument(0))));
        when(reembolsoRepository.save(any(Reembolso.class))).thenAnswer(a -> {
            Reembolso r = a.getArgument(0);
            if (r.getId() == null) { r.setId(siguienteReembolso++); r.setSolicitadoEn(LocalDateTime.now()); }
            reembolsos.put(r.getId(), r);
            return r;
        });
        when(reembolsoRepository.findById(anyInt())).thenAnswer(a -> Optional.ofNullable(reembolsos.get(a.<Integer>getArgument(0))));
        // ¿la línea del pago está en una solicitud con esos estados? (se calcula con las solicitudes guardadas)
        when(reembolsoInscripcionRepository.existsByPagoInscripcion_IdAndReembolso_EstadoIn(anyInt(), anyCollection())).thenAnswer(a -> {
            Integer linea = a.getArgument(0); Collection<String> estados = a.getArgument(1);
            return reembolsos.values().stream().filter(r -> estados.contains(r.getEstado()))
                    .anyMatch(r -> r.getItems().stream().anyMatch(it -> it.getPagoInscripcion().getId().equals(linea)));
        });
        when(inscripcionService.avance(any(Inscripcion.class))).thenAnswer(a -> avances.getOrDefault(a.<Inscripcion>getArgument(0).getId(), new BigDecimal("5.00")));
        when(consecutivoRepository.bloquear(anyInt())).thenReturn(Optional.empty());
        when(notaCreditoRepository.save(any(NotaCredito.class))).thenAnswer(a -> {
            NotaCredito n = a.getArgument(0);
            if (n.getId() == null) { n.setId(300 + notas.size()); n.setFechaEmision(LocalDateTime.now()); notas.add(n); }
            return n;
        });
        when(notaCreditoRepository.findByReembolso_Id(anyInt())).thenAnswer(a -> notas.stream().filter(n -> n.getReembolso().getId().equals(a.getArgument(0))).findFirst());
        when(notaCreditoRepository.findById(anyInt())).thenAnswer(a -> notas.stream().filter(n -> n.getId().equals(a.getArgument(0))).findFirst());
        when(datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc()).thenReturn(Optional.of(
                DatosFacturacion.builder().id(1).razonSocial("BioLearn S.A.S.").nit("901000000-0").build()));
        when(pasarela.reembolsar(any(Pago.class), any(BigDecimal.class))).thenReturn("PRUEBA-REV-1234ABCD");
        when(archivoFirma.firmar(any())).thenAnswer(a -> a.getArgument(0) == null ? null : a.getArgument(0) + "?exp=1&firma=x");
    }

    private SolicitudReembolsoRequest solicitud(int idPago, Integer... ids) {
        return SolicitudReembolsoRequest.builder().idPago(idPago).idsInscripcion(List.of(ids)).motivo("Me equivoqué de curso al pagar").build();
    }

    private SolicitudReembolsoRequest conCuenta(SolicitudReembolsoRequest r) {
        r.setMedioDevolucion("cuenta_bancaria"); r.setBanco("Bancolombia"); r.setTipoCuenta("ahorros");
        r.setNumeroCuenta("123-456-7890"); r.setTitular("Esteban Valencia"); r.setDocumentoTitular("1001234567");
        return r;
    }

    // ── opciones y solicitud ──
    @Nested @DisplayName("Opciones y solicitud del estudiante")
    class Solicitud {

        @Test @DisplayName("Opciones: plazo de 8 días y cursos reembolsables; el de más del 20 % aparece con el motivo")
        void opciones() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now().minusDays(2), est, 101, 102, 103);
            avances.put(103, new BigDecimal("35.00"));
            OpcionesReembolsoResponse o = servicio.opciones(1, estU);
            assertThat(o.getEnPlazo()).isTrue();
            assertThat(o.getPlazoHasta()).isEqualTo(p.getFechaPago().plusDays(8));
            assertThat(o.getDevolucionAlMismoMedio()).isTrue();
            assertThat(o.getCursos()).extracting(OpcionesReembolsoResponse.Curso::getReembolsable).containsExactly(true, true, false);
            assertThat(o.getCursos().get(2).getMotivo()).contains("35 %").contains("menos del 20 %");
        }

        @Test @DisplayName("Dos de tres cursos con tarjeta: una solicitud, monto de los dos y cursos en pausa")
        void dosDeTres() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now().minusDays(1), est, 101, 102, 103);
            ReembolsoResponse r = servicio.solicitar(solicitud(1, 101, 102), estU);
            assertThat(r.getEstado()).isEqualTo("solicitado");
            assertThat(r.getMonto()).isEqualByComparingTo("100000");
            assertThat(r.getItems()).extracting(ReembolsoResponse.Item::getIdInscripcion).containsExactly(101, 102);
            assertThat(r.getMedioDevolucion()).isEqualTo("mismo_medio");
            assertThat(r.getMedioDevolucionNombre()).isEqualTo("Mismo medio (Tarjeta)");
            assertThat(insc(p, 101).getEstado()).isEqualTo("en_reembolso");
            assertThat(insc(p, 103).getEstado()).isEqualTo("activa");
            assertThat(r.getEstudiante()).isNull();                          // el estudiante no recibe datos de otros
            verify(notificacionRepository).save(argThat(n -> "reembolso".equals(n.getTipo())));
        }

        @Test @DisplayName("Un curso retirado se puede reembolsar y vuelve a retirado si se cancela la solicitud")
        void retiradoYCancelar() {
            Pago p = pago(1, "wompi_nequi", LocalDateTime.now().minusDays(1), est, 101, 102);
            insc(p, 102).setEstado("retirada");
            ReembolsoResponse r = servicio.solicitar(solicitud(1, 101, 102), estU);
            assertThat(insc(p, 102).getEstado()).isEqualTo("retirada");
            ReembolsoResponse c = servicio.cancelar(r.getId(), estU);
            assertThat(c.getEstado()).isEqualTo("cancelado");
            assertThat(insc(p, 101).getEstado()).isEqualTo("activa");
            assertThat(insc(p, 102).getEstado()).isEqualTo("retirada");
            assertThatThrownBy(() -> servicio.cancelar(r.getId(), estU)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("ya fue resuelta");
        }

        @Test @DisplayName("Plazo vencido, pago no aprobado o ya reembolsado: 409")
        void reglasDelPago() {
            pago(1, "wompi_tarjeta", LocalDateTime.now().minusDays(8).minusMinutes(1), est, 101);
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 101), estU)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("venció");
            Pago p2 = pago(2, "wompi_tarjeta", LocalDateTime.now(), est, 201);
            p2.setEstado("pendiente");
            assertThatThrownBy(() -> servicio.solicitar(solicitud(2, 201), estU)).hasMessageContaining("Solo se reembolsan pagos aprobados");
            p2.setEstado("reembolsado");
            assertThatThrownBy(() -> servicio.solicitar(solicitud(2, 201), estU)).hasMessageContaining("ya fue reembolsado por completo");
        }

        @Test @DisplayName("Avance del 20 % o más, curso completado o con otra solicitud en trámite: 409")
        void reglasDelCurso() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now(), est, 101, 102, 103);
            avances.put(101, new BigDecimal("20.00"));
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 101), estU)).isInstanceOf(SolicitudInvalidaException.class)
                    .hasMessageContaining("«Curso 101»").hasMessageContaining("20 %");
            insc(p, 102).setEstado("completada");
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 102), estU)).hasMessageContaining("Ya completaste este curso");
            servicio.solicitar(solicitud(1, 103), estU);
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 103), estU)).hasMessageContaining("solicitud de reembolso en trámite");
        }

        @Test @DisplayName("Curso que no es del pago, cursos repetidos o pago de otro estudiante")
        void datosNoValidos() {
            pago(1, "wompi_tarjeta", LocalDateTime.now(), est, 101);
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 999), estU)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no pertenece a este pago");
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 101, 101), estU)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("repetidos");
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 101), otroU)).isInstanceOf(SinPermisoException.class).hasMessage("Este pago no es tuyo");
            assertThatThrownBy(() -> servicio.solicitar(solicitud(77, 101), estU)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test @DisplayName("PSE o transferencia exigen cuenta bancaria o Nequi válidos; tarjeta no admite otro medio")
        void datosDeDevolucion() {
            pago(1, "transferencia", LocalDateTime.now(), est, 101);
            assertThatThrownBy(() -> servicio.solicitar(solicitud(1, 101), estU)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("indica dónde recibir la devolución");
            SolicitudReembolsoRequest mala = solicitud(1, 101);
            mala.setMedioDevolucion("cuenta_bancaria"); mala.setTipoCuenta("nomina"); mala.setNumeroCuenta("12");
            assertThatThrownBy(() -> servicio.solicitar(mala, estU)).hasMessageContaining("Escribe el banco")
                    .hasMessageContaining("ahorros o corriente").hasMessageContaining("entre 6 y 20 dígitos")
                    .hasMessageContaining("titular").hasMessageContaining("documento del titular");
            SolicitudReembolsoRequest nequi = solicitud(1, 101);
            nequi.setMedioDevolucion("nequi"); nequi.setTelefonoNequi("2001234567");
            assertThatThrownBy(() -> servicio.solicitar(nequi, estU)).hasMessageContaining("empezar por 3");

            ReembolsoResponse r = servicio.solicitar(conCuenta(solicitud(1, 101)), estU);
            assertThat(r.getMedioDevolucion()).isEqualTo("cuenta_bancaria");
            assertThat(r.getNumeroCuenta()).isEqualTo("••••7890");             // enmascarado para el estudiante
            assertThat(reembolsos.get(r.getId()).getNumeroCuenta()).isEqualTo("1234567890");

            pago(2, "wompi_tarjeta", LocalDateTime.now(), est, 201);
            SolicitudReembolsoRequest otraCuenta = conCuenta(solicitud(2, 201));
            assertThatThrownBy(() -> servicio.solicitar(otraCuenta, estU)).hasMessageContaining("mismo medio con que pagaste (Tarjeta)");
        }
    }

    // ── aprobación automática ──
    @Nested @DisplayName("Aprobación automática (todos los cursos con 0 % de avance y dentro del plazo)")
    class AprobacionAutomatica {

        @Test @DisplayName("Tarjeta con 0 % en los cursos elegidos: se aprueba al solicitar, se devuelve por la pasarela y se emite la nota crédito")
        void tarjetaSinAvance() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now().minusDays(1), est, 101, 102, 103);
            avances.put(101, BigDecimal.ZERO); avances.put(102, new BigDecimal("0.00"));
            ReembolsoResponse r = servicio.solicitar(solicitud(1, 101, 102), estU);
            assertThat(r.getEstado()).isEqualTo("devuelto");
            assertThat(r.getAprobacionAutomatica()).isTrue();
            assertThat(r.getReferenciaDevolucion()).isEqualTo("PRUEBA-REV-1234ABCD");
            assertThat(r.getNumeroNotaCredito()).endsWith("-000001");
            assertThat(insc(p, 101).getEstado()).isEqualTo("retirada");
            assertThat(insc(p, 103).getEstado()).isEqualTo("activa");
            assertThat(servicio.reembolso(r.getId(), contU).getResueltoPor()).isEqualTo("Aprobación automática");
            verify(pasarela).reembolsar(p, new BigDecimal("100000"));
            verify(notificacionRepository).save(argThat(n -> "Reembolso aprobado automáticamente".equals(n.getTitulo()) && n.getEmisor() == null));
            verify(correoService).notaCredito(eq("30@t.co"), any(), any(), any(), any(), eq("$100.000"), any());
        }

        @Test @DisplayName("Transferencia con 0 %: queda aprobada (por devolver) y el contador registra la devolución")
        void transferenciaSinAvance() {
            pago(1, "transferencia", LocalDateTime.now(), est, 101);
            avances.put(101, BigDecimal.ZERO);
            ReembolsoResponse r = servicio.solicitar(conCuenta(solicitud(1, 101)), estU);
            assertThat(r.getEstado()).isEqualTo("aprobado");
            assertThat(r.getAprobacionAutomatica()).isTrue();
            verify(pasarela, never()).reembolsar(any(), any());
            ReembolsoResponse d = servicio.registrarDevolucion(r.getId(), new DevolucionRequest("TRF-1", null), contU);
            assertThat(d.getEstado()).isEqualTo("devuelto");
            assertThat(d.getResueltoPor()).isEqualTo("N40 A");                // quien hizo la devolución
            assertThat(d.getAprobacionAutomatica()).isTrue();
        }

        @Test @DisplayName("Si algún curso elegido tiene avance (aunque sea menor del 20 %), la revisa un contador")
        void conAvanceSeRevisa() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now(), est, 101, 102);
            avances.put(101, BigDecimal.ZERO); avances.put(102, new BigDecimal("10.00"));
            ReembolsoResponse r = servicio.solicitar(solicitud(1, 101, 102), estU);
            assertThat(r.getEstado()).isEqualTo("solicitado");
            assertThat(r.getAprobacionAutomatica()).isFalse();
            assertThat(insc(p, 101).getEstado()).isEqualTo("en_reembolso");
            verify(pasarela, never()).reembolsar(any(), any());
        }
    }

    // ── resolución del contador ──
    @Nested @DisplayName("Resolución del contador")
    class Resolucion {

        @Test @DisplayName("Aprobar con tarjeta: devolución automática, nota crédito con los cursos, cursos retirados y correo")
        void aprobarTarjeta() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now().minusDays(1), est, 101, 102, 103);
            Integer id = servicio.solicitar(solicitud(1, 101, 102), estU).getId();
            ReembolsoResponse r = servicio.aprobar(id, contU);
            assertThat(r.getEstado()).isEqualTo("devuelto");
            assertThat(r.getReferenciaDevolucion()).isEqualTo("PRUEBA-REV-1234ABCD");
            assertThat(r.getNumeroNotaCredito()).isEqualTo("BIO-NC-" + java.time.LocalDate.now().getYear() + "-000001");
            assertThat(r.getResueltoPor()).isEqualTo("N40 A");
            assertThat(insc(p, 101).getEstado()).isEqualTo("retirada");
            assertThat(insc(p, 103).getEstado()).isEqualTo("activa");
            assertThat(p.getEstado()).isEqualTo("aprobado");                  // queda un curso sin reembolsar
            NotaCredito n = notas.get(0);
            assertThat(n.getMontoTotal()).isEqualByComparingTo("100000");
            assertThat(n.getFactura().getNumero()).isEqualTo("BIO-FAC-2026-00001");
            assertThat(n.getEnviadaPorCorreo()).isTrue();
            verify(pasarela).reembolsar(p, new BigDecimal("100000"));
            verify(correoService).notaCredito(eq("30@t.co"), eq("N30"), eq(n.getNumero()), eq("BIO-FAC-2026-00001"), contains("Curso 102"), eq("$100.000"), contains("PRUEBA-REV-1234ABCD"));
        }

        @Test @DisplayName("Cuando todos los cursos quedan reembolsados el pago pasa a reembolsado; la segunda nota sigue el consecutivo")
        void pagoReembolsado() {
            Pago p = pago(1, "wompi_nequi", LocalDateTime.now(), est, 101, 102);
            servicio.aprobar(servicio.solicitar(solicitud(1, 101), estU).getId(), contU);
            when(consecutivoRepository.bloquear(anyInt())).thenReturn(Optional.of(ConsecutivoNotaCredito.builder().anio(2026).ultimo(1).build()));
            ReembolsoResponse r2 = servicio.aprobar(servicio.solicitar(solicitud(1, 102), estU).getId(), contU);
            assertThat(p.getEstado()).isEqualTo("reembolsado");
            assertThat(r2.getNumeroNotaCredito()).endsWith("-000002");
            verify(pagoRepository).save(p);
        }

        @Test @DisplayName("Aprobar una transferencia: queda aprobado hasta registrar la devolución con su referencia")
        void aprobarTransferenciaYDevolver() {
            pago(1, "transferencia", LocalDateTime.now(), est, 101);
            Integer id = servicio.solicitar(conCuenta(solicitud(1, 101)), estU).getId();
            ReembolsoResponse r = servicio.aprobar(id, contU);
            assertThat(r.getEstado()).isEqualTo("aprobado");
            assertThat(r.getNumeroCuenta()).isEqualTo("1234567890");          // completa para quien hace la devolución
            verify(pasarela, never()).reembolsar(any(), any());
            assertThatThrownBy(() -> servicio.registrarDevolucion(id, new DevolucionRequest("REF-1", "https://otro.sitio/c.pdf"), contU))
                    .isInstanceOf(IllegalArgumentException.class);
            ReembolsoResponse d = servicio.registrarDevolucion(id,
                    new DevolucionRequest(" TRF-998877 ", "/api/archivos/0f8fad5b-d9cb-469f-a165-70867728950e.pdf"), contU);
            assertThat(d.getEstado()).isEqualTo("devuelto");
            assertThat(d.getReferenciaDevolucion()).isEqualTo("TRF-998877");
            assertThat(d.getComprobanteDevolucionUrl()).endsWith("?exp=1&firma=x");
            verify(correoService).avisoReembolso(eq("30@t.co"), eq("N30"), eq("Te devolvimos tu dinero"),
                    argThat(m -> m.contains("TRF-998877") && m.contains("a la cuenta de ahorros " + PUNTOS_HTML + "7890 de Bancolombia") && !m.contains("1234567890")));
            verify(correoService).notaCredito(any(), any(), any(), any(), any(), any(),
                    argThat(pie -> pie.contains("Te devolveremos el dinero a la cuenta de ahorros " + PUNTOS_HTML + "7890") && !pie.contains("1234567890")));
            Reembolso nequi = Reembolso.builder().medioDevolucion("nequi").telefonoNequi("3001234567").build();
            assertThat(ReembolsoServiceImpl.aDestino(nequi)).isEqualTo("al Nequi ••••4567");
            assertThat(ReembolsoServiceImpl.destino(nequi)).isEqualTo("Nequi ••••4567");
            assertThatThrownBy(() -> servicio.registrarDevolucion(id, new DevolucionRequest("X", null), contU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("estado devuelto");
        }

        @Test @DisplayName("Rechazar devuelve los cursos a su estado y avisa al estudiante con el motivo")
        void rechazar() {
            Pago p = pago(1, "wompi_tarjeta", LocalDateTime.now(), est, 101);
            Integer id = servicio.solicitar(solicitud(1, 101), estU).getId();
            ReembolsoResponse r = servicio.rechazar(id, new RechazoPagoRequest("El curso ya fue revisado casi completo"), contU);
            assertThat(r.getEstado()).isEqualTo("rechazado");
            assertThat(r.getJustificacion()).isEqualTo("El curso ya fue revisado casi completo");
            assertThat(insc(p, 101).getEstado()).isEqualTo("activa");
            assertThat(notas).isEmpty();
            verify(correoService).avisoReembolso(eq("30@t.co"), any(), eq("Solicitud de reembolso rechazada"), contains("casi completo"));
            assertThatThrownBy(() -> servicio.aprobar(id, contU)).isInstanceOf(SolicitudInvalidaException.class);
        }

        @Test @DisplayName("El auditor consulta con la cuenta enmascarada pero no aprueba, rechaza, devuelve ni reenvía (403)")
        void auditor() {
            pago(1, "pse", LocalDateTime.now(), est, 101);
            Integer id = servicio.solicitar(conCuenta(solicitud(1, 101)), estU).getId();
            when(reembolsoRepository.findAllByOrderBySolicitadoEnDesc()).thenReturn(new ArrayList<>(reembolsos.values()));
            ReembolsoResponse visto = servicio.reembolsos("solicitado", "", null, null, auditorU).get(0);
            assertThat(visto.getNumeroCuenta()).isEqualTo("••••7890");
            assertThat(visto.getDocumentoTitular()).isEqualTo("••••4567");
            assertThat(visto.getEstudiante()).isEqualTo("N30 A");
            assertThat(servicio.reembolso(id, contU).getNumeroCuenta()).isEqualTo("1234567890");
            assertThatThrownBy(() -> servicio.aprobar(id, auditorU)).isInstanceOf(SinPermisoException.class).hasMessageContaining("auditor");
            assertThatThrownBy(() -> servicio.rechazar(id, new RechazoPagoRequest("x"), auditorU)).isInstanceOf(SinPermisoException.class);
            assertThatThrownBy(() -> servicio.enviarNotaCredito(1, auditorU)).isInstanceOf(SinPermisoException.class);
            assertThatThrownBy(() -> servicio.reembolsos(null, null, null, null, estU)).isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("Filtros: estado no válido y fechas al revés → 400; búsqueda sin tildes por estudiante o curso")
        void filtros() {
            pago(1, "wompi_tarjeta", LocalDateTime.now(), est, 101);
            servicio.solicitar(solicitud(1, 101), estU);
            when(reembolsoRepository.findAllByOrderBySolicitadoEnDesc()).thenReturn(new ArrayList<>(reembolsos.values()));
            assertThatThrownBy(() -> servicio.reembolsos("pagado", null, null, null, contU)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.reembolsos(null, null, java.time.LocalDate.now(), java.time.LocalDate.now().minusDays(1), contU))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(servicio.reembolsos(null, "CURSO 101", null, null, contU)).hasSize(1);
            assertThat(servicio.reembolsos(null, "nadie", null, null, contU)).isEmpty();
        }

        @Test @DisplayName("Nota crédito: el estudiante ve la suya, no la de otro; inexistente 404")
        void notaDelEstudiante() {
            pago(1, "wompi_tarjeta", LocalDateTime.now(), est, 101);
            servicio.aprobar(servicio.solicitar(solicitud(1, 101), estU).getId(), contU);
            Integer idNota = notas.get(0).getId();
            NotaCreditoResponse n = servicio.miNotaCredito(idNota, estU);
            assertThat(n.getNumeroFactura()).isEqualTo("BIO-FAC-2026-00001");
            assertThat(n.getItems()).hasSize(1);
            assertThat(n.getRazonSocial()).isEqualTo("BioLearn S.A.S.");
            assertThatThrownBy(() -> servicio.miNotaCredito(idNota, otroU)).isInstanceOf(SinPermisoException.class).hasMessage("Esta nota crédito no es tuya");
            assertThatThrownBy(() -> servicio.miNotaCredito(999, estU)).isInstanceOf(RecursoNoEncontradoException.class);
        }
    }
}
