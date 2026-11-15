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
import co.biolearn.service.PasarelaPago;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PagoServiceImpl — carrito, pago y factura (Pagos, Fase 1)")
class PagoServiceImplTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private PagoInscripcionRepository pagoInscripcionRepository;
    @Mock private FacturaRepository facturaRepository;
    @Mock private DatosFacturacionRepository datosFacturacionRepository;
    @Mock private ConsecutivoFacturaRepository consecutivoRepository;
    @Mock private NotificacionRepository notificacionRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private PerfilEstudianteRepository perfilEstudianteRepository;
    @Mock private PerfilContadorRepository perfilContadorRepository;
    @Mock private InscripcionServiceImpl inscripcionService;
    @Mock private PasarelaPago pasarela;
    @Mock private ArchivoFirma archivoFirma;

    @Mock private co.biolearn.service.CorreoService correoService;

    @InjectMocks private PagoServiceImpl servicio;

    private Usuario estU, otroU, contU, auditorU;
    private PerfilEstudiante est, otro;
    private PerfilContador contador;
    private Curso c1, c2, gratis;
    private final Map<Integer, Pago> pagos = new HashMap<>();
    private final List<Factura> facturas = new ArrayList<>();
    private final Set<Integer> enPagoPendiente = new HashSet<>();
    private int siguientePago = 500;

    private static Usuario usuario(int id, RolUsuario rol) {
        return Usuario.builder().id(id).nombre("N" + id).apellido("A").correo(id + "@t.co").tipoUsuario(rol).estado(EstadoUsuario.activo).build();
    }

    private static Curso curso(int id, String precio) {
        return Curso.builder().id(id).nombre("Curso " + id).codigo("C-" + id).estado("activo").precio(new BigDecimal(precio)).build();
    }

    private Inscripcion inscripcion(int id, Curso c, PerfilEstudiante e, String estado) {
        Inscripcion i = Inscripcion.builder().id(id).curso(c).estudiante(e).estado(estado).build();
        when(inscripcionRepository.findById(id)).thenReturn(Optional.of(i));
        return i;
    }

    @BeforeEach
    void datos() {
        estU = usuario(30, RolUsuario.estudiante); otroU = usuario(31, RolUsuario.estudiante);
        contU = usuario(40, RolUsuario.contador); auditorU = usuario(41, RolUsuario.contador);
        est = PerfilEstudiante.builder().id(9).usuario(estU).build();
        otro = PerfilEstudiante.builder().id(10).usuario(otroU).build();
        when(perfilEstudianteRepository.findByUsuario_Id(30)).thenReturn(Optional.of(est));
        when(perfilEstudianteRepository.findByUsuario_Id(31)).thenReturn(Optional.of(otro));
        contador = PerfilContador.builder().id(3).usuario(contU).rolContador(RolContador.auxiliar).estado(EstadoPerfil.activo).build();
        when(perfilContadorRepository.findByUsuario_Id(40)).thenReturn(Optional.of(contador));
        when(perfilContadorRepository.findByUsuario_Id(41)).thenReturn(Optional.of(
                PerfilContador.builder().id(4).usuario(auditorU).rolContador(RolContador.auditor).estado(EstadoPerfil.activo).build()));
        c1 = curso(11, "95000"); c2 = curso(12, "50000"); gratis = curso(13, "0");
        when(pagoRepository.save(any(Pago.class))).thenAnswer(a -> {
            Pago p = a.getArgument(0);
            if (p.getId() == null) { p.setId(siguientePago++); p.setCreadoEn(LocalDateTime.now()); }
            pagos.put(p.getId(), p);
            if ("pendiente".equals(p.getEstado())) p.getItems().forEach(it -> enPagoPendiente.add(it.getInscripcion().getId()));
            else p.getItems().forEach(it -> enPagoPendiente.remove(it.getInscripcion().getId()));
            return p;
        });
        when(pagoRepository.findById(anyInt())).thenAnswer(a -> Optional.ofNullable(pagos.get(a.<Integer>getArgument(0))));
        when(pagoInscripcionRepository.existsByInscripcion_IdAndPago_EstadoIn(anyInt(), anyCollection()))
                .thenAnswer(a -> enPagoPendiente.contains(a.<Integer>getArgument(0)));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(a -> {
            Factura f = a.getArgument(0);
            if (f.getId() == null) { f.setId(700 + facturas.size()); f.setFechaEmision(LocalDateTime.now()); facturas.add(f); }
            return f;
        });
        when(facturaRepository.findByPago_Id(anyInt())).thenAnswer(a -> facturas.stream().filter(f -> f.getPago().getId().equals(a.getArgument(0))).findFirst());
        when(facturaRepository.findById(anyInt())).thenAnswer(a -> facturas.stream().filter(f -> f.getId().equals(a.getArgument(0))).findFirst());
        when(consecutivoRepository.bloquear(anyInt())).thenReturn(Optional.empty());
        when(datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc()).thenReturn(Optional.of(
                DatosFacturacion.builder().id(1).razonSocial("BioLearn S.A.S.").nit("901000000-0").build()));
        when(archivoFirma.firmar(any())).thenAnswer(a -> a.getArgument(0) == null ? null : a.getArgument(0) + "?exp=1&firma=x");
    }

    private PagoResponse crear(String medio, Integer... ids) {
        return servicio.crearPago(new CrearPagoRequest(List.of(ids), medio), estU);
    }

    // ── checkout ──
    @Nested @DisplayName("Carrito y creación del pago")
    class Checkout {

        @Test @DisplayName("El carrito lista las inscripciones pendientes de pago y separa las que ya tienen un pago en proceso")
        void carrito() {
            Inscripcion a = inscripcion(100, c1, est, "pendiente_pago"), b = inscripcion(101, c2, est, "pendiente_pago");
            Inscripcion activa = inscripcion(102, gratis, est, "activa");
            when(inscripcionRepository.findByEstudiante_IdOrderByFechaInscripcionDesc(9)).thenReturn(List.of(a, b, activa));
            enPagoPendiente.add(101);
            CarritoResponse r = servicio.carrito(estU);
            assertThat(r.getItems()).extracting(PagoResponse.Item::getIdInscripcion).containsExactly(100);
            assertThat(r.getEnProceso()).extracting(PagoResponse.Item::getIdInscripcion).containsExactly(101);
            assertThat(r.getTotal()).isEqualByComparingTo("95000");
        }

        @Test @DisplayName("Un pago con dos cursos suma sus precios y queda pendiente")
        void crearPago() {
            inscripcion(100, c1, est, "pendiente_pago"); inscripcion(101, c2, est, "pendiente_pago");
            PagoResponse r = crear("tarjeta", 100, 101);
            assertThat(r.getEstado()).isEqualTo("pendiente");
            assertThat(r.getMonto()).isEqualByComparingTo("145000");
            assertThat(r.getMedio()).isEqualTo("wompi_tarjeta");
            assertThat(r.getEnLinea()).isTrue();
            assertThat(r.getItems()).hasSize(2);
        }

        @Test @DisplayName("Medio de pago inválido → 400; cursos repetidos → 400")
        void medioInvalido() {
            inscripcion(100, c1, est, "pendiente_pago");
            assertThatThrownBy(() -> crear("bitcoin", 100)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Medio de pago no válido");
            assertThatThrownBy(() -> crear("pse", 100, 100)).isInstanceOf(IllegalArgumentException.class).hasMessage("Hay cursos repetidos en el carrito");
        }

        @Test @DisplayName("Inscripción de otro estudiante → 403; inexistente → 404")
        void ajena() {
            inscripcion(100, c1, otro, "pendiente_pago");
            assertThatThrownBy(() -> crear("pse", 100)).isInstanceOf(SinPermisoException.class).hasMessage("Esta inscripción no es tuya");
            assertThatThrownBy(() -> crear("pse", 999)).isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test @DisplayName("Inscripción que no está pendiente de pago → 409; curso gratuito → 409")
        void noPendiente() {
            inscripcion(100, c1, est, "activa");
            assertThatThrownBy(() -> crear("pse", 100)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("no está pendiente de pago");
            inscripcion(101, gratis, est, "pendiente_pago");
            assertThatThrownBy(() -> crear("pse", 101)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("es gratuito");
        }

        @Test @DisplayName("Una inscripción no puede estar en dos pagos pendientes → 409")
        void dosPagos() {
            inscripcion(100, c1, est, "pendiente_pago");
            crear("transferencia", 100);
            assertThatThrownBy(() -> crear("tarjeta", 100)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("ya tiene un pago en proceso");
        }
    }

    // ── pasarela ──
    @Nested @DisplayName("Pago en línea")
    class EnLinea {

        @Test @DisplayName("Aprobado: activa las inscripciones, emite la factura BIO-FAC-AAAA-000001, la envía y notifica")
        void aprobado() {
            Inscripcion i = inscripcion(100, c1, est, "pendiente_pago");
            Integer id = crear("tarjeta", 100).getId();
            when(pasarela.cobrar(any(), any())).thenReturn(PasarelaPago.Resultado.aprobado("PRUEBA-ABC"));
            PagoResponse r = servicio.pagarEnPasarela(id, new PagoPasarelaRequest("4242424242424242", null, null), estU);
            assertThat(r.getEstado()).isEqualTo("aprobado");
            assertThat(r.getReferencia()).isEqualTo("PRUEBA-ABC");
            assertThat(r.getNumeroFactura()).isEqualTo("BIO-FAC-" + LocalDate.now().getYear() + "-000001");
            verify(inscripcionService).activarPorPago(i);
            assertThat(facturas.get(0).getEnviadaPorCorreo()).isTrue();
            assertThat(facturas.get(0).getDatosFacturacion().getNit()).isEqualTo("901000000-0");
            assertThat(facturas.get(0).getMontoTotal()).isEqualByComparingTo("95000");
            verify(notificacionRepository).save(argThat(n -> "pago".equals(n.getTipo()) && n.getTitulo().equals("Pago aprobado") && n.getEntidadId().equals(id)));
            verify(correoService).factura(eq("30@t.co"), eq("N30"), startsWith("BIO-FAC-"), contains("Curso 11"), eq("$95.000"), contains("901000000-0"));
        }

        @Test @DisplayName("El consecutivo continúa desde el último número del año")
        void consecutivo() {
            inscripcion(100, c1, est, "pendiente_pago");
            Integer id = crear("pse", 100).getId();
            when(consecutivoRepository.bloquear(anyInt())).thenReturn(Optional.of(ConsecutivoFactura.builder().anio(LocalDate.now().getYear()).ultimo(41).build()));
            when(pasarela.cobrar(any(), any())).thenReturn(PasarelaPago.Resultado.aprobado("PRUEBA-1"));
            assertThat(servicio.pagarEnPasarela(id, new PagoPasarelaRequest(null, null, "1"), estU).getNumeroFactura()).endsWith("-000042");
        }

        @Test @DisplayName("Rechazado: guarda el motivo, no activa ni factura, y la inscripción vuelve al carrito")
        void rechazado() {
            inscripcion(100, c1, est, "pendiente_pago");
            Integer id = crear("nequi", 100).getId();
            when(pasarela.cobrar(any(), any())).thenReturn(PasarelaPago.Resultado.rechazado("PRUEBA-2", "El pago no fue aceptado en Nequi"));
            PagoResponse r = servicio.pagarEnPasarela(id, new PagoPasarelaRequest(null, "3992222222", null), estU);
            assertThat(r.getEstado()).isEqualTo("rechazado");
            assertThat(r.getMotivoRechazo()).isEqualTo("El pago no fue aceptado en Nequi");
            assertThat(facturas).isEmpty();
            verify(inscripcionService, never()).activarPorPago(any());
            assertThat(enPagoPendiente).doesNotContain(100);
        }

        @Test @DisplayName("Pagar dos veces → 409; pagar en la pasarela una transferencia → 409; pago ajeno → 403")
        void invalidos() {
            inscripcion(100, c1, est, "pendiente_pago"); inscripcion(101, c2, est, "pendiente_pago");
            Integer id = crear("tarjeta", 100).getId();
            when(pasarela.cobrar(any(), any())).thenReturn(PasarelaPago.Resultado.aprobado("R"));
            servicio.pagarEnPasarela(id, new PagoPasarelaRequest(), estU);
            assertThatThrownBy(() -> servicio.pagarEnPasarela(id, new PagoPasarelaRequest(), estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("ya fue procesado");
            Integer t = crear("transferencia", 101).getId();
            assertThatThrownBy(() -> servicio.pagarEnPasarela(t, new PagoPasarelaRequest(), estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("por transferencia");
            assertThatThrownBy(() -> servicio.pagarEnPasarela(t, new PagoPasarelaRequest(), otroU))
                    .isInstanceOf(SinPermisoException.class).hasMessage("Este pago no es tuyo");
        }
    }

    // ── transferencia ──
    @Nested @DisplayName("Transferencia y contador")
    class Transferencia {

        private Integer transferenciaConComprobante() {
            inscripcion(100, c1, est, "pendiente_pago");
            Integer id = crear("transferencia", 100).getId();
            servicio.adjuntarComprobante(id, new ComprobanteRequest("/api/archivos/0f8fad5b-d9cb-469f-a165-70867728950e.pdf?exp=9&firma=abc"), estU);
            return id;
        }

        @Test @DisplayName("El comprobante se guarda sin la firma y se devuelve firmado")
        void comprobante() {
            Integer id = transferenciaConComprobante();
            assertThat(pagos.get(id).getComprobanteUrl()).isEqualTo("/api/archivos/0f8fad5b-d9cb-469f-a165-70867728950e.pdf");
            assertThat(servicio.miPago(id, estU).getComprobanteUrl()).endsWith("?exp=1&firma=x");
        }

        @Test @DisplayName("Comprobante que no es un archivo de la plataforma → 400; en un pago en línea → 409")
        void comprobanteInvalido() {
            inscripcion(100, c1, est, "pendiente_pago"); inscripcion(101, c2, est, "pendiente_pago");
            Integer id = crear("transferencia", 100).getId();
            assertThatThrownBy(() -> servicio.adjuntarComprobante(id, new ComprobanteRequest("https://otro.sitio/x.pdf"), estU))
                    .isInstanceOf(IllegalArgumentException.class);
            Integer enLinea = crear("tarjeta", 101).getId();
            assertThatThrownBy(() -> servicio.adjuntarComprobante(enLinea, new ComprobanteRequest("/api/archivos/0f8fad5b-d9cb-469f-a165-70867728950e.png"), estU))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("Solo los pagos por transferencia llevan comprobante");
        }

        @Test @DisplayName("El contador confirma: aprobado con el contador que lo resolvió y la factura emitida")
        void confirmar() {
            Integer id = transferenciaConComprobante();
            PagoResponse r = servicio.confirmar(id, contU);
            assertThat(r.getEstado()).isEqualTo("aprobado");
            assertThat(r.getResueltoPor()).isEqualTo("N40 A");
            assertThat(r.getNumeroFactura()).startsWith("BIO-FAC-");
            assertThat(pagos.get(id).getFechaPago()).isNotNull();
        }

        @Test @DisplayName("Confirmar sin comprobante → 409; un pago en línea → 409; el auditor → 403")
        void confirmarInvalido() {
            inscripcion(100, c1, est, "pendiente_pago"); inscripcion(101, c2, est, "pendiente_pago");
            Integer sinComp = crear("transferencia", 100).getId();
            assertThatThrownBy(() -> servicio.confirmar(sinComp, contU)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("comprobante");
            Integer enLinea = crear("pse", 101).getId();
            assertThatThrownBy(() -> servicio.confirmar(enLinea, contU)).isInstanceOf(SolicitudInvalidaException.class).hasMessageContaining("los confirma la pasarela");
            assertThatThrownBy(() -> servicio.confirmar(sinComp, auditorU)).isInstanceOf(SinPermisoException.class).hasMessage("El rol auditor solo consulta pagos y facturas");
        }

        @Test @DisplayName("El contador rechaza con motivo; la notificación lo incluye")
        void rechazar() {
            Integer id = transferenciaConComprobante();
            PagoResponse r = servicio.rechazar(id, new RechazoPagoRequest("El valor no coincide"), contU);
            assertThat(r.getEstado()).isEqualTo("rechazado");
            assertThat(r.getMotivoRechazo()).isEqualTo("El valor no coincide");
            verify(notificacionRepository).save(argThat(n -> n.getContenido().contains("El valor no coincide")));
        }

        @Test @DisplayName("El estudiante cancela un pago pendiente; uno ya resuelto → 409")
        void cancelar() {
            Integer id = transferenciaConComprobante();
            assertThat(servicio.cancelar(id, estU).getEstado()).isEqualTo("cancelado");
            assertThat(enPagoPendiente).doesNotContain(100);
            assertThatThrownBy(() -> servicio.cancelar(id, estU)).isInstanceOf(SolicitudInvalidaException.class);
        }
    }

    // ── consultas ──
    @Nested @DisplayName("Consultas y facturas")
    class Consultas {

        @Test @DisplayName("El contador filtra por estado, medio y búsqueda sin tildes; un estado inválido → 400")
        void filtros() {
            inscripcion(100, c1, est, "pendiente_pago"); inscripcion(101, c2, est, "pendiente_pago");
            crear("transferencia", 100); crear("tarjeta", 101);
            when(pagoRepository.findAllByOrderByCreadoEnDesc()).thenAnswer(a -> new ArrayList<>(pagos.values()));
            assertThat(servicio.pagos("pendiente", "transferencia", null, null, null)).hasSize(1);
            assertThat(servicio.pagos(null, null, "curso 12", null, null)).hasSize(1).first().extracting(PagoResponse::getEstudiante).isEqualTo("N30 A");
            assertThatThrownBy(() -> servicio.pagos("pagado", null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> servicio.pagos(null, null, null, LocalDate.now(), LocalDate.now().minusDays(1))).isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("El estudiante ve su factura; la de otro → 403; el contador la reenvía y el auditor no")
        void facturas() {
            inscripcion(100, c1, est, "pendiente_pago");
            Integer id = crear("tarjeta", 100).getId();
            when(pasarela.cobrar(any(), any())).thenReturn(PasarelaPago.Resultado.aprobado("R"));
            Integer idFactura = servicio.pagarEnPasarela(id, new PagoPasarelaRequest(), estU).getIdFactura();
            FacturaResponse f = servicio.miFactura(idFactura, estU);
            assertThat(f.getRazonSocial()).isEqualTo("BioLearn S.A.S.");
            assertThat(f.getItems()).extracting(PagoResponse.Item::getCurso).containsExactly("Curso 11");
            assertThatThrownBy(() -> servicio.miFactura(idFactura, otroU)).isInstanceOf(SinPermisoException.class).hasMessage("Esta factura no es tuya");
            assertThat(servicio.enviarFactura(idFactura, contU).getEnviadaPorCorreo()).isTrue();
            assertThatThrownBy(() -> servicio.enviarFactura(idFactura, auditorU)).isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("Permisos: el auxiliar resuelve pagos; el auditor solo consulta; un estudiante → 403")
        void permisos() {
            assertThat(servicio.permisos(contU).getPuedeResolver()).isTrue();
            PermisosContadorResponse au = servicio.permisos(auditorU);
            assertThat(au.getRolContador()).isEqualTo("auditor");
            assertThat(au.getPuedeResolver()).isFalse();
            assertThatThrownBy(() -> servicio.permisos(estU)).isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("Formato de pesos colombianos en los mensajes")
        void pesos() {
            assertThat(PagoServiceImpl.pesos(new BigDecimal("145000"))).isEqualTo("$145.000");
        }
    }
}
