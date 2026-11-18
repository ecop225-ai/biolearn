package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfil;
import co.biolearn.enums.RolContador;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.ArchivoFirma;
import co.biolearn.service.CorreoService;
import co.biolearn.service.PagoService;
import co.biolearn.service.PasarelaPago;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Módulo de Pagos, Fase 1: carrito, pago y factura.
 * - El carrito son las inscripciones del estudiante en pendiente_pago que no están en un pago pendiente.
 * - Un pago cubre una o varias inscripciones; cada línea guarda el precio del curso al pagar.
 * - En línea (tarjeta, PSE, Nequi): lo resuelve la pasarela. Transferencia: el estudiante adjunta el comprobante
 *   y un contador (administrador financiero o auxiliar; el auditor solo consulta) lo confirma o lo rechaza.
 * - Aprobado: inscripciones activas, factura con número consecutivo y los datos de facturación vigentes (RNF-021),
 *   envío por correo y notificación al estudiante. Rechazado o cancelado: las inscripciones siguen pendientes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    /** Medio recibido en la API → valor de la columna medio_pago. */
    static final Map<String, String> MEDIOS = Map.of(
            "tarjeta", "wompi_tarjeta", "pse", "pse", "nequi", "wompi_nequi", "transferencia", "transferencia");
    static final Map<String, String> NOMBRES_MEDIO = Map.of(
            "wompi_tarjeta", "Tarjeta", "pse", "PSE", "wompi_nequi", "Nequi", "transferencia", "Transferencia", "otro", "Otro");
    static final Set<String> EN_LINEA = Set.of("wompi_tarjeta", "pse", "wompi_nequi");
    static final Set<String> ESTADOS = Set.of("pendiente", "aprobado", "rechazado", "cancelado", "reembolsado");
    private static final Pattern ARCHIVO = Pattern.compile("^/api/archivos/([0-9a-f-]{36}\\.(pdf|png|jpg))(\\?.*)?$");

    private final PagoRepository pagoRepository;
    private final PagoInscripcionRepository pagoInscripcionRepository;
    private final FacturaRepository facturaRepository;
    private final DatosFacturacionRepository datosFacturacionRepository;
    private final ConsecutivoFacturaRepository consecutivoRepository;
    private final NotificacionRepository notificacionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final PerfilContadorRepository perfilContadorRepository;
    private final InscripcionServiceImpl inscripcionService;
    private final PasarelaPago pasarela;
    private final ArchivoFirma archivoFirma;
    private final CorreoService correoService;

    // ── estudiante ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CarritoResponse carrito(Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        List<PagoResponse.Item> libres = new ArrayList<>(), enProceso = new ArrayList<>();
        for (Inscripcion i : inscripcionRepository.findByEstudiante_IdOrderByFechaInscripcionDesc(est.getId())) {
            if (!"pendiente_pago".equals(i.getEstado())) continue;
            (enPagoPendiente(i) ? enProceso : libres).add(item(i, i.getCurso().getPrecio()));
        }
        BigDecimal total = libres.stream().map(PagoResponse.Item::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        return CarritoResponse.builder().items(libres).total(total).enProceso(enProceso).build();
    }

    @Override
    @Transactional
    public PagoResponse crearPago(CrearPagoRequest req, Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        String medio = MEDIOS.get(req.getMedio() == null ? "" : req.getMedio().trim().toLowerCase());
        if (medio == null) throw new IllegalArgumentException("Medio de pago no válido. Usa: tarjeta, pse, nequi o transferencia");
        if (new HashSet<>(req.getIdsInscripcion()).size() != req.getIdsInscripcion().size())
            throw new IllegalArgumentException("Hay cursos repetidos en el carrito");
        Pago pago = Pago.builder().estudiante(est).medioPago(medio).estado("pendiente").monto(BigDecimal.ZERO).build();
        BigDecimal total = BigDecimal.ZERO;
        for (Integer id : req.getIdsInscripcion()) {
            Inscripcion i = inscripcionRepository.findById(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("La inscripción indicada no existe"));
            if (!i.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Esta inscripción no es tuya");
            String curso = i.getCurso().getNombre();
            if (!"pendiente_pago".equals(i.getEstado()))
                throw new SolicitudInvalidaException("El curso «" + curso + "» no está pendiente de pago");
            if (enPagoPendiente(i))
                throw new SolicitudInvalidaException("El curso «" + curso + "» ya tiene un pago en proceso");
            BigDecimal precio = i.getCurso().getPrecio();
            if (precio == null || precio.signum() <= 0)
                throw new SolicitudInvalidaException("El curso «" + curso + "» es gratuito; no requiere pago");
            pago.getItems().add(PagoInscripcion.builder().pago(pago).inscripcion(i).montoAsignado(precio).build());
            total = total.add(precio);
        }
        pago.setMonto(total);
        return aPago(pagoRepository.save(pago), false);
    }

    @Override
    @Transactional
    public PagoResponse pagarEnPasarela(Integer idPago, PagoPasarelaRequest datos, Usuario usuario) {
        Pago pago = pagoPropio(idPago, usuario);
        if (!EN_LINEA.contains(pago.getMedioPago()))
            throw new SolicitudInvalidaException("Este pago es por transferencia: adjunta el comprobante para que lo confirme un contador");
        exigirPendiente(pago);
        PasarelaPago.Resultado r = pasarela.cobrar(pago, datos);
        pago.setReferenciaExterna(r.referencia());
        if (r.aprobado()) aprobar(pago, null);
        else rechazarPago(pago, r.motivo(), null);
        return aPago(pago, false);
    }

    @Override
    @Transactional
    public PagoResponse adjuntarComprobante(Integer idPago, ComprobanteRequest req, Usuario usuario) {
        Pago pago = pagoPropio(idPago, usuario);
        if (!"transferencia".equals(pago.getMedioPago()))
            throw new SolicitudInvalidaException("Solo los pagos por transferencia llevan comprobante");
        exigirPendiente(pago);
        Matcher m = ARCHIVO.matcher(req.getUrl().trim());
        if (!m.matches()) throw new IllegalArgumentException("El comprobante debe ser un archivo subido a la plataforma (PDF, PNG o JPG)");
        pago.setComprobanteUrl("/api/archivos/" + m.group(1));            // se guarda sin la firma
        return aPago(pagoRepository.save(pago), false);
    }

    @Override
    @Transactional
    public PagoResponse cancelar(Integer idPago, Usuario usuario) {
        Pago pago = pagoPropio(idPago, usuario);
        exigirPendiente(pago);
        pago.setEstado("cancelado");
        return aPago(pagoRepository.save(pago), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> misPagos(Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        return pagoRepository.findByEstudiante_IdOrderByCreadoEnDesc(est.getId()).stream().map(p -> aPago(p, false)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse miPago(Integer idPago, Usuario usuario) {
        return aPago(pagoPropio(idPago, usuario), false);
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponse miFactura(Integer idFactura, Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        Factura f = buscarFactura(idFactura);
        if (!f.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Esta factura no es tuya");
        return aFactura(f);
    }

    // ── contador ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> pagos(String estado, String medio, String busqueda, LocalDate desde, LocalDate hasta) {
        if (estado != null && !estado.isBlank() && !ESTADOS.contains(estado))
            throw new IllegalArgumentException("Estado no válido. Usa: pendiente, aprobado, rechazado, cancelado o reembolsado");
        String medioCol = null;
        if (medio != null && !medio.isBlank()) {
            medioCol = MEDIOS.get(medio.trim().toLowerCase());
            if (medioCol == null) throw new IllegalArgumentException("Medio de pago no válido. Usa: tarjeta, pse, nequi o transferencia");
        }
        validarRango(desde, hasta);
        String q = normalizar(busqueda), mc = medioCol;
        return pagoRepository.findAllByOrderByCreadoEnDesc().stream()
                .filter(p -> estado == null || estado.isBlank() || estado.equals(p.getEstado()))
                .filter(p -> mc == null || mc.equals(p.getMedioPago()))
                .filter(p -> enRango(p.getCreadoEn(), desde, hasta))
                .filter(p -> q.isEmpty() || normalizar(nombre(p.getEstudiante().getUsuario()) + " " + p.getEstudiante().getUsuario().getCorreo()
                        + " " + Objects.toString(p.getReferenciaExterna(), "") + " "
                        + String.join(" ", p.getItems().stream().map(x -> x.getInscripcion().getCurso().getNombre()).toList())).contains(q))
                .map(p -> aPago(p, true)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse pago(Integer idPago) { return aPago(buscarPago(idPago), true); }

    @Override
    @Transactional
    public PagoResponse confirmar(Integer idPago, Usuario usuario) {
        PerfilContador contador = contadorQueResuelve(usuario);
        Pago pago = buscarPago(idPago);
        exigirTransferenciaPendiente(pago);
        if (pago.getComprobanteUrl() == null)
            throw new SolicitudInvalidaException("El estudiante aún no ha adjuntado el comprobante de la transferencia");
        aprobar(pago, contador);
        return aPago(pago, true);
    }

    @Override
    @Transactional
    public PagoResponse rechazar(Integer idPago, RechazoPagoRequest req, Usuario usuario) {
        PerfilContador contador = contadorQueResuelve(usuario);
        Pago pago = buscarPago(idPago);
        exigirTransferenciaPendiente(pago);
        rechazarPago(pago, req.getMotivo().trim(), contador);
        return aPago(pago, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacturaResponse> facturas(String busqueda, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        String q = normalizar(busqueda);
        return facturaRepository.findAllByOrderByFechaEmisionDesc().stream()
                .filter(f -> enRango(f.getFechaEmision(), desde, hasta))
                .filter(f -> q.isEmpty() || normalizar(f.getNumero() + " " + nombre(f.getEstudiante().getUsuario()) + " "
                        + f.getEstudiante().getUsuario().getCorreo() + " " + Objects.toString(f.getDescripcion(), "")).contains(q))
                .map(this::aFactura).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FacturaResponse factura(Integer idFactura) { return aFactura(buscarFactura(idFactura)); }

    @Override
    @Transactional
    public FacturaResponse enviarFactura(Integer idFactura, Usuario usuario) {
        contadorQueResuelve(usuario);
        Factura f = buscarFactura(idFactura);
        enviarPorCorreo(f);
        return aFactura(f);
    }

    /** El portal oculta Confirmar, Rechazar y Reenviar al auditor (el servidor igual los rechaza con 403). */
    @Override
    @Transactional(readOnly = true)
    public PermisosContadorResponse permisos(Usuario usuario) {
        PerfilContador c = perfilContadorRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los contadores"));
        boolean puede = c.getEstado() == EstadoPerfil.activo && c.getRolContador() != RolContador.auditor;
        return PermisosContadorResponse.builder().rolContador(c.getRolContador().name()).puedeResolver(puede).build();
    }

    // ── reglas ──────────────────────────────────────────────────────────────

    /** Aprueba el pago: activa las inscripciones, emite la factura, la envía y notifica al estudiante. */
    void aprobar(Pago pago, PerfilContador contador) {
        pago.setEstado("aprobado");
        pago.setFechaPago(LocalDateTime.now());
        pago.setContador(contador);
        pago.setMotivoRechazo(null);
        pagoRepository.save(pago);
        pago.getItems().forEach(it -> inscripcionService.activarPorPago(it.getInscripcion()));
        Factura f = emitirFactura(pago);
        enviarPorCorreo(f);
        notificar(pago, "Pago aprobado", "Tu pago de " + pesos(pago.getMonto()) + " fue aprobado. Ya puedes ver tus cursos. Factura " + f.getNumero() + ".", contador);
    }

    private void rechazarPago(Pago pago, String motivo, PerfilContador contador) {
        pago.setEstado("rechazado");
        pago.setMotivoRechazo(motivo);
        pago.setContador(contador);
        pagoRepository.save(pago);
        notificar(pago, "Pago rechazado", "Tu pago de " + pesos(pago.getMonto()) + " fue rechazado: " + motivo + ". Puedes intentarlo de nuevo desde el carrito.", contador);
    }

    /** Factura con número BIO-FAC-AAAA-000001; la fila del consecutivo se bloquea para no repetir números. */
    Factura emitirFactura(Pago pago) {
        int anio = LocalDate.now().getYear();
        ConsecutivoFactura c = consecutivoRepository.bloquear(anio).orElseGet(() -> ConsecutivoFactura.builder().anio(anio).ultimo(0).build());
        c.setUltimo(c.getUltimo() + 1);
        consecutivoRepository.save(c);
        DatosFacturacion datos = datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc().orElse(null);
        String descripcion = "Inscripción a: " + String.join(", ", pago.getItems().stream().map(i -> i.getInscripcion().getCurso().getNombre()).toList());
        Factura f = Factura.builder().pago(pago).estudiante(pago.getEstudiante()).datosFacturacion(datos)
                .numero(String.format("BIO-FAC-%d-%06d", anio, c.getUltimo()))
                .descripcion(descripcion).montoTotal(pago.getMonto()).montoPagado(pago.getMonto()).estado("emitida").build();
        return facturaRepository.save(f);
    }

    /** Envía la factura al correo del estudiante (si el correo no está configurado, queda en la consola). */
    private void enviarPorCorreo(Factura f) {
        Usuario u = f.getEstudiante().getUsuario();
        log.info("Factura {} enviada por correo a {} ({})", f.getNumero(), u.getCorreo(), pesos(f.getMontoTotal()));
        StringBuilder filas = new StringBuilder();
        f.getPago().getItems().forEach(it -> filas.append("<tr><td style=\"padding:8px;border-bottom:1px solid #E0E2E8\">Inscripción — ")
                .append(org.springframework.web.util.HtmlUtils.htmlEscape(it.getInscripcion().getCurso().getNombre()))
                .append("</td><td style=\"padding:8px;border-bottom:1px solid #E0E2E8;text-align:right\">")
                .append(pesos(it.getMontoAsignado())).append("</td></tr>"));
        DatosFacturacion d = f.getDatosFacturacion();
        String pie = (d != null ? org.springframework.web.util.HtmlUtils.htmlEscape(d.getRazonSocial() + " · NIT " + d.getNit()) + " · " : "")
                + "Medio de pago: " + NOMBRES_MEDIO.getOrDefault(f.getPago().getMedioPago(), f.getPago().getMedioPago())
                + (f.getPago().getReferenciaExterna() != null ? " · Ref. " + org.springframework.web.util.HtmlUtils.htmlEscape(f.getPago().getReferenciaExterna()) : "");
        correoService.factura(u.getCorreo(), u.getNombre(), f.getNumero(), filas.toString(), pesos(f.getMontoTotal()), pie);
        f.setEnviadaPorCorreo(true);
        f.setEnviadaEn(LocalDateTime.now());
        facturaRepository.save(f);
    }

    private void notificar(Pago pago, String titulo, String contenido, PerfilContador contador) {
        notificacionRepository.save(Notificacion.builder().usuario(pago.getEstudiante().getUsuario())
                .emisor(contador != null ? contador.getUsuario() : null)
                .tipo("pago").entidadId(pago.getId()).titulo(titulo).contenido(contenido).build());
    }

    private boolean enPagoPendiente(Inscripcion i) {
        return pagoInscripcionRepository.existsByInscripcion_IdAndPago_EstadoIn(i.getId(), List.of("pendiente"));
    }

    private static void exigirPendiente(Pago pago) {
        if (!"pendiente".equals(pago.getEstado())) throw new SolicitudInvalidaException("El pago ya fue procesado (estado " + pago.getEstado() + ")");
    }

    private static void exigirTransferenciaPendiente(Pago pago) {
        if (EN_LINEA.contains(pago.getMedioPago())) throw new SolicitudInvalidaException("Los pagos en línea los confirma la pasarela, no un contador");
        exigirPendiente(pago);
    }

    /** Contador activo que puede resolver pagos: administrador financiero o auxiliar (el auditor solo consulta). */
    private PerfilContador contadorQueResuelve(Usuario usuario) {
        PerfilContador c = perfilContadorRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los contadores"));
        if (c.getEstado() != EstadoPerfil.activo) throw new SinPermisoException("Tu perfil de contador no está activo");
        if (c.getRolContador() == RolContador.auditor) throw new SinPermisoException("El rol auditor solo consulta pagos y facturas");
        return c;
    }

    private Pago pagoPropio(Integer idPago, Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        Pago p = buscarPago(idPago);
        if (!p.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Este pago no es tuyo");
        return p;
    }

    private Pago buscarPago(Integer id) {
        return pagoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El pago indicado no existe"));
    }

    private Factura buscarFactura(Integer id) {
        return facturaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La factura indicada no existe"));
    }

    private PerfilEstudiante perfilEstudiante(Usuario usuario) {
        return perfilEstudianteRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los estudiantes"));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
    }

    private static boolean enRango(LocalDateTime f, LocalDate desde, LocalDate hasta) {
        if (f == null) return desde == null && hasta == null;
        LocalDate d = f.toLocalDate();
        return (desde == null || !d.isBefore(desde)) && (hasta == null || !d.isAfter(hasta));
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private static String nombre(Usuario u) { return u.getNombre() + " " + u.getApellido(); }

    static String pesos(BigDecimal v) {
        return "$" + String.format(Locale.forLanguageTag("es-CO"), "%,.0f", v == null ? BigDecimal.ZERO : v);
    }

    // ── conversión ──────────────────────────────────────────────────────────

    private static PagoResponse.Item item(Inscripcion i, BigDecimal monto) {
        Curso c = i.getCurso();
        return PagoResponse.Item.builder().idInscripcion(i.getId()).idCurso(c.getId()).curso(c.getNombre()).codigoCurso(c.getCodigo()).monto(monto)
                .renovacion("pendiente_pago".equals(i.getEstado()) && i.getAccesoDesde() != null).build();
    }

    private PagoResponse aPago(Pago p, boolean conEstudiante) {
        Optional<Factura> f = p.getId() == null ? Optional.empty() : facturaRepository.findByPago_Id(p.getId());
        Usuario u = p.getEstudiante().getUsuario();
        return PagoResponse.builder()
                .id(p.getId()).medio(p.getMedioPago()).medioNombre(NOMBRES_MEDIO.getOrDefault(p.getMedioPago(), p.getMedioPago()))
                .enLinea(EN_LINEA.contains(p.getMedioPago())).estado(p.getEstado()).monto(p.getMonto())
                .creadoEn(p.getCreadoEn()).fechaPago(p.getFechaPago()).referencia(p.getReferenciaExterna()).motivoRechazo(p.getMotivoRechazo())
                .comprobanteUrl(archivoFirma.firmar(p.getComprobanteUrl()))
                .items(p.getItems().stream().map(x -> item(x.getInscripcion(), x.getMontoAsignado())).toList())
                .idFactura(f.map(Factura::getId).orElse(null)).numeroFactura(f.map(Factura::getNumero).orElse(null))
                .idEstudiante(conEstudiante ? p.getEstudiante().getId() : null)
                .estudiante(conEstudiante ? nombre(u) : null).correo(conEstudiante ? u.getCorreo() : null)
                .resueltoPor(conEstudiante && p.getContador() != null ? nombre(p.getContador().getUsuario()) : null)
                .build();
    }

    private FacturaResponse aFactura(Factura f) {
        Pago p = f.getPago();
        DatosFacturacion d = f.getDatosFacturacion();
        Usuario u = f.getEstudiante().getUsuario();
        return FacturaResponse.builder()
                .id(f.getId()).numero(f.getNumero()).fechaEmision(f.getFechaEmision()).estado(f.getEstado())
                .razonSocial(d != null ? d.getRazonSocial() : null).nit(d != null ? d.getNit() : null).direccionEmisor(FinanzasServiceImpl.direccionCompleta(d))
                .idEstudiante(f.getEstudiante().getId()).estudiante(nombre(u)).correo(u.getCorreo()).documento(u.getDocIdentidad())
                .descripcion(f.getDescripcion())
                .items(p.getItems().stream().map(x -> item(x.getInscripcion(), x.getMontoAsignado())).toList())
                .montoTotal(f.getMontoTotal()).montoPagado(f.getMontoPagado())
                .idPago(p.getId()).medioNombre(NOMBRES_MEDIO.getOrDefault(p.getMedioPago(), p.getMedioPago())).referencia(p.getReferenciaExterna())
                .enviadaPorCorreo(f.getEnviadaPorCorreo()).enviadaEn(f.getEnviadaEn())
                .build();
    }
}
