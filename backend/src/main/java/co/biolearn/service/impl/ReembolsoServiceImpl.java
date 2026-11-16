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
import co.biolearn.service.PasarelaPago;
import co.biolearn.service.ReembolsoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Módulo de Pagos, Fase 2: reembolsos con nota crédito.
 * - El estudiante pide el reembolso de uno o varios cursos de un pago aprobado: dentro de los 8 días siguientes al pago
 *   y con menos del 20 % de avance en cada curso. Una sola solicitud por vez para cada curso pagado.
 * - Mientras se revisa, el curso queda en pausa (inscripción en_reembolso) para que el avance no cambie.
 * - Devolución: tarjeta y Nequi vuelven al mismo medio por la pasarela al aprobar (queda devuelto). PSE y transferencia:
 *   el estudiante indica una cuenta bancaria o un Nequi; el contador hace la devolución y la registra (aprobado → devuelto).
 * - Aprobar (administrador financiero o auxiliar; el auditor solo consulta): los cursos quedan retirados y se emite una
 *   nota crédito BIO-NC-AAAA-000001 sobre la factura del pago, con una línea por curso. Si todos los cursos del pago
 *   quedan reembolsados, el pago pasa a reembolsado. Rechazar o cancelar devuelve los cursos a su estado anterior.
 * - Aprobación automática: si todos los cursos elegidos tienen 0 % de avance (y la solicitud está en plazo), se aprueba
 *   al solicitarla, sin pasar por el contador. Con tarjeta o Nequi el dinero se devuelve de inmediato; con PSE o
 *   transferencia queda «por devolver» y el contador registra la devolución. Con avance entre 0 y 20 % la revisa un contador.
 * - Los números de cuenta solo se muestran completos al contador que puede hacer la devolución.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReembolsoServiceImpl implements ReembolsoService {

    static final int DIAS_PLAZO = 8;
    static final BigDecimal AVANCE_MAXIMO = new BigDecimal("20");
    /** Medios que la pasarela devuelve automáticamente al mismo medio. */
    static final Set<String> AL_MISMO_MEDIO = Set.of("wompi_tarjeta", "wompi_nequi");
    static final Set<String> VIGENTES = Set.of("solicitado", "aprobado", "devuelto");
    static final Set<String> ESTADOS = Set.of("solicitado", "aprobado", "devuelto", "rechazado", "cancelado");
    static final Map<String, String> NOMBRES_DEVOLUCION = Map.of(
            "mismo_medio", "Mismo medio de pago", "cuenta_bancaria", "Cuenta bancaria", "nequi", "Nequi");
    private static final Pattern ARCHIVO = Pattern.compile("^/api/archivos/([0-9a-f-]{36}\\.(pdf|png|jpg))(\\?.*)?$");

    private final ReembolsoRepository reembolsoRepository;
    private final ReembolsoInscripcionRepository reembolsoInscripcionRepository;
    private final NotaCreditoRepository notaCreditoRepository;
    private final ConsecutivoNotaCreditoRepository consecutivoRepository;
    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final DatosFacturacionRepository datosFacturacionRepository;
    private final InscripcionRepository inscripcionRepository;
    private final NotificacionRepository notificacionRepository;
    private final PerfilEstudianteRepository perfilEstudianteRepository;
    private final PerfilContadorRepository perfilContadorRepository;
    private final InscripcionServiceImpl inscripcionService;
    private final PasarelaPago pasarela;
    private final ArchivoFirma archivoFirma;
    private final CorreoService correoService;

    // ── estudiante ──────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OpcionesReembolsoResponse opciones(Integer idPago, Usuario usuario) {
        Pago pago = pagoPropio(idPago, usuario);
        String motivoPago = motivoPagoNoReembolsable(pago);
        List<OpcionesReembolsoResponse.Curso> cursos = pago.getItems().stream().map(it -> {
            BigDecimal avance = inscripcionService.avance(it.getInscripcion());
            String motivo = motivoPago != null ? motivoPago : motivoCursoNoReembolsable(it, avance);
            Curso c = it.getInscripcion().getCurso();
            return OpcionesReembolsoResponse.Curso.builder().idInscripcion(it.getInscripcion().getId()).curso(c.getNombre())
                    .codigoCurso(c.getCodigo()).monto(it.getMontoAsignado()).avance(avance)
                    .estadoInscripcion(it.getInscripcion().getEstado()).reembolsable(motivo == null).motivo(motivo).build();
        }).toList();
        return OpcionesReembolsoResponse.builder().idPago(pago.getId()).medioNombre(PagoServiceImpl.NOMBRES_MEDIO.getOrDefault(pago.getMedioPago(), pago.getMedioPago()))
                .fechaPago(pago.getFechaPago()).plazoHasta(plazo(pago)).enPlazo(enPlazo(pago))
                .devolucionAlMismoMedio(AL_MISMO_MEDIO.contains(pago.getMedioPago()))
                .diasPlazo(DIAS_PLAZO).avanceMaximo(AVANCE_MAXIMO).cursos(cursos).build();
    }

    @Override
    @Transactional
    public ReembolsoResponse solicitar(SolicitudReembolsoRequest req, Usuario usuario) {
        Pago pago = pagoPropio(req.getIdPago(), usuario);
        if (new HashSet<>(req.getIdsInscripcion()).size() != req.getIdsInscripcion().size())
            throw new IllegalArgumentException("Hay cursos repetidos en la solicitud");
        Reembolso r = Reembolso.builder().pago(pago).solicitante(usuario).motivo(req.getMotivo().trim()).estado("solicitado").build();
        String motivoPago = motivoPagoNoReembolsable(pago);
        if (motivoPago != null) throw new SolicitudInvalidaException(motivoPago);
        datosDevolucion(r, pago, req);                                   // 400 si faltan datos de la cuenta o del Nequi

        BigDecimal total = BigDecimal.ZERO;
        for (Integer idInscripcion : req.getIdsInscripcion()) {
            PagoInscripcion linea = pago.getItems().stream().filter(x -> x.getInscripcion().getId().equals(idInscripcion)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El curso indicado (inscripción " + idInscripcion + ") no pertenece a este pago"));
            Inscripcion insc = linea.getInscripcion();
            BigDecimal avance = inscripcionService.avance(insc);
            String motivo = motivoCursoNoReembolsable(linea, avance);
            if (motivo != null) throw new SolicitudInvalidaException("«" + insc.getCurso().getNombre() + "»: " + motivo);
            r.getItems().add(ReembolsoInscripcion.builder().reembolso(r).pagoInscripcion(linea).monto(linea.getMontoAsignado())
                    .avance(avance).estadoInscripcionPrevio(insc.getEstado()).build());
            total = total.add(linea.getMontoAsignado());
        }
        r.setMonto(total);
        reembolsoRepository.save(r);
        if (aprobableAutomaticamente(r)) {                              // casos simples: sin avance y dentro del plazo
            r.setAprobacionAutomatica(true);
            aprobarSolicitud(r, null);
            return aReembolso(r, Vista.ESTUDIANTE);
        }
        r.getItems().forEach(it -> {                                    // el curso queda en pausa mientras se revisa
            Inscripcion insc = it.getPagoInscripcion().getInscripcion();
            if ("activa".equals(insc.getEstado())) { insc.setEstado("en_reembolso"); inscripcionRepository.save(insc); }
        });
        notificar(r, "Solicitud de reembolso recibida", "Recibimos tu solicitud de reembolso por " + PagoServiceImpl.pesos(total)
                + ". Mientras la revisamos, " + (r.getItems().size() == 1 ? "el curso queda en pausa." : "los cursos quedan en pausa."), null);
        return aReembolso(r, Vista.ESTUDIANTE);
    }

    /**
     * Aprobación automática: todos los cursos de la solicitud tienen 0 % de avance (el plazo ya se validó).
     * Con avance mayor que 0 y menor que el 20 % la solicitud la revisa un contador.
     */
    static boolean aprobableAutomaticamente(Reembolso r) {
        return !r.getItems().isEmpty() && r.getItems().stream().allMatch(it -> it.getAvance() == null || it.getAvance().signum() == 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReembolsoResponse> misReembolsos(Usuario usuario) {
        perfilEstudiante(usuario);
        return reembolsoRepository.findBySolicitante_IdOrderBySolicitadoEnDesc(usuario.getId()).stream()
                .map(r -> aReembolso(r, Vista.ESTUDIANTE)).toList();
    }

    @Override
    @Transactional
    public ReembolsoResponse cancelar(Integer idReembolso, Usuario usuario) {
        Reembolso r = buscar(idReembolso);
        PerfilEstudiante est = perfilEstudiante(usuario);
        if (!r.getPago().getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Esta solicitud de reembolso no es tuya");
        exigirSolicitado(r);
        r.setEstado("cancelado");
        r.setResueltoEn(LocalDateTime.now());
        reembolsoRepository.save(r);
        restaurarCursos(r);
        return aReembolso(r, Vista.ESTUDIANTE);
    }

    @Override
    @Transactional(readOnly = true)
    public NotaCreditoResponse miNotaCredito(Integer idNota, Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        NotaCredito n = buscarNota(idNota);
        if (!n.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Esta nota crédito no es tuya");
        return aNota(n);
    }

    // ── contador ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReembolsoResponse> reembolsos(String estado, String busqueda, LocalDate desde, LocalDate hasta, Usuario usuario) {
        Vista vista = vistaContador(usuario);
        if (estado != null && !estado.isBlank() && !ESTADOS.contains(estado))
            throw new IllegalArgumentException("Estado no válido. Usa: solicitado, aprobado, devuelto, rechazado o cancelado");
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        String q = normalizar(busqueda);
        return reembolsoRepository.findAllByOrderBySolicitadoEnDesc().stream()
                .filter(r -> estado == null || estado.isBlank() || estado.equals(r.getEstado()))
                .filter(r -> enRango(r.getSolicitadoEn(), desde, hasta))
                .filter(r -> q.isEmpty() || normalizar(textoBusqueda(r)).contains(q))
                .map(r -> aReembolso(r, vista)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReembolsoResponse reembolso(Integer idReembolso, Usuario usuario) {
        return aReembolso(buscar(idReembolso), vistaContador(usuario));
    }

    @Override
    @Transactional
    public ReembolsoResponse aprobar(Integer idReembolso, Usuario usuario) {
        PerfilContador contador = contadorQueResuelve(usuario);
        Reembolso r = buscar(idReembolso);
        exigirSolicitado(r);
        aprobarSolicitud(r, contador);
        return aReembolso(r, Vista.COMPLETA);
    }

    /** Aprueba (contador = null: aprobación automática), devuelve por la pasarela si aplica, emite la nota crédito y avisa. */
    private void aprobarSolicitud(Reembolso r, PerfilContador contador) {
        Pago pago = r.getPago();
        Factura factura = facturaRepository.findByPago_Id(pago.getId())
                .orElseThrow(() -> new SolicitudInvalidaException("El pago no tiene factura; no se puede emitir la nota crédito"));
        r.setEstado("aprobado");
        r.setContador(contador);
        r.setResueltoEn(LocalDateTime.now());
        r.getItems().forEach(it -> {                                    // los cursos reembolsados quedan retirados (se conserva el progreso)
            Inscripcion insc = it.getPagoInscripcion().getInscripcion();
            insc.setEstado("retirada");
            inscripcionRepository.save(insc);
        });
        if ("mismo_medio".equals(r.getMedioDevolucion())) {             // tarjeta o Nequi: la pasarela devuelve al mismo medio
            r.setReferenciaDevolucion(pasarela.reembolsar(pago, r.getMonto()));
            r.setDevueltoEn(LocalDateTime.now());
            r.setEstado("devuelto");
        }
        reembolsoRepository.save(r);
        actualizarEstadoPago(pago);
        NotaCredito nota = emitirNota(r, factura);
        enviarNota(nota);
        notificar(r, contador == null ? "Reembolso aprobado automáticamente" : "Reembolso aprobado",
                "Tu reembolso de " + PagoServiceImpl.pesos(r.getMonto()) + " fue aprobado" + (contador == null ? " automáticamente porque no tenías avance en " + (r.getItems().size() == 1 ? "el curso" : "los cursos") : "")
                + " (nota crédito " + nota.getNumero() + "). "
                + ("devuelto".equals(r.getEstado()) ? "El dinero se devolvió a tu " + medioPagoNombre(pago) + "." : "Te devolveremos el dinero " + aDestino(r) + "."), contador);
    }

    @Override
    @Transactional
    public ReembolsoResponse rechazar(Integer idReembolso, RechazoPagoRequest req, Usuario usuario) {
        PerfilContador contador = contadorQueResuelve(usuario);
        Reembolso r = buscar(idReembolso);
        exigirSolicitado(r);
        String motivo = req.getMotivo().trim();
        r.setEstado("rechazado");
        r.setJustificacion(motivo);
        r.setContador(contador);
        r.setResueltoEn(LocalDateTime.now());
        reembolsoRepository.save(r);
        restaurarCursos(r);
        notificar(r, "Reembolso rechazado", "Tu solicitud de reembolso fue rechazada: " + motivo + ". Ya puedes continuar con tus cursos.", contador);
        Usuario u = r.getPago().getEstudiante().getUsuario();
        correoService.avisoReembolso(u.getCorreo(), u.getNombre(), "Solicitud de reembolso rechazada",
                "Revisamos tu solicitud de reembolso por <strong>" + PagoServiceImpl.pesos(r.getMonto()) + "</strong> y no fue aprobada.<br>Motivo: "
                        + HtmlUtils.htmlEscape(motivo) + "<br>Tus cursos siguen disponibles.");
        return aReembolso(r, Vista.COMPLETA);
    }

    @Override
    @Transactional
    public ReembolsoResponse registrarDevolucion(Integer idReembolso, DevolucionRequest req, Usuario usuario) {
        PerfilContador contador = contadorQueResuelve(usuario);
        Reembolso r = buscar(idReembolso);
        if (!"aprobado".equals(r.getEstado()))
            throw new SolicitudInvalidaException("Solo se registra la devolución de un reembolso aprobado (estado " + r.getEstado() + ")");
        if ("mismo_medio".equals(r.getMedioDevolucion()))
            throw new SolicitudInvalidaException("Este reembolso lo devuelve la pasarela al mismo medio de pago");
        if (req.getComprobanteUrl() != null && !req.getComprobanteUrl().isBlank()) {
            Matcher m = ARCHIVO.matcher(req.getComprobanteUrl().trim());
            if (!m.matches()) throw new IllegalArgumentException("El comprobante debe ser un archivo subido a la plataforma (PDF, PNG o JPG)");
            r.setComprobanteDevolucionUrl("/api/archivos/" + m.group(1));
        }
        r.setReferenciaDevolucion(req.getReferencia().trim());
        r.setDevueltoEn(LocalDateTime.now());
        r.setEstado("devuelto");
        if (r.getContador() == null) r.setContador(contador);
        reembolsoRepository.save(r);
        String destino = aDestino(r);
        notificar(r, "Dinero devuelto", "Te devolvimos " + PagoServiceImpl.pesos(r.getMonto()) + " " + destino + ". Referencia " + r.getReferenciaDevolucion() + ".", contador);
        Usuario u = r.getPago().getEstudiante().getUsuario();
        correoService.avisoReembolso(u.getCorreo(), u.getNombre(), "Te devolvimos tu dinero",
                "Te devolvimos <strong>" + PagoServiceImpl.pesos(r.getMonto()) + "</strong> " + HtmlUtils.htmlEscape(destino)
                        + ".<br>Referencia de la transacción: " + HtmlUtils.htmlEscape(r.getReferenciaDevolucion()) + ".");
        return aReembolso(r, Vista.COMPLETA);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotaCreditoResponse> notasCredito(String busqueda, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        String q = normalizar(busqueda);
        return notaCreditoRepository.findAllByOrderByFechaEmisionDesc().stream()
                .filter(n -> enRango(n.getFechaEmision(), desde, hasta))
                .filter(n -> q.isEmpty() || normalizar(n.getNumero() + " " + n.getFactura().getNumero() + " " + textoBusqueda(n.getReembolso())).contains(q))
                .map(this::aNota).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotaCreditoResponse notaCredito(Integer idNota) { return aNota(buscarNota(idNota)); }

    @Override
    @Transactional
    public NotaCreditoResponse enviarNotaCredito(Integer idNota, Usuario usuario) {
        contadorQueResuelve(usuario);
        NotaCredito n = buscarNota(idNota);
        enviarNota(n);
        return aNota(n);
    }

    // ── reglas ──────────────────────────────────────────────────────────────

    private static LocalDateTime plazo(Pago p) { return p.getFechaPago() == null ? null : p.getFechaPago().plusDays(DIAS_PLAZO); }

    private static boolean enPlazo(Pago p) { return p.getFechaPago() != null && !LocalDateTime.now().isAfter(plazo(p)); }

    /** null si el pago admite reembolsos; si no, el motivo para el estudiante. */
    private static String motivoPagoNoReembolsable(Pago pago) {
        if ("reembolsado".equals(pago.getEstado())) return "Este pago ya fue reembolsado por completo";
        if (!"aprobado".equals(pago.getEstado())) return "Solo se reembolsan pagos aprobados (este pago está " + pago.getEstado() + ")";
        if (!enPlazo(pago)) return "El plazo para pedir el reembolso venció: eran " + DIAS_PLAZO + " días desde el pago";
        return null;
    }

    /** null si el curso se puede reembolsar; si no, el motivo. */
    private String motivoCursoNoReembolsable(PagoInscripcion linea, BigDecimal avance) {
        if (reembolsoInscripcionRepository.existsByPagoInscripcion_IdAndReembolso_EstadoIn(linea.getId(), List.of("solicitado")))
            return "Ya tiene una solicitud de reembolso en trámite";
        if (reembolsoInscripcionRepository.existsByPagoInscripcion_IdAndReembolso_EstadoIn(linea.getId(), List.of("aprobado", "devuelto")))
            return "Ya fue reembolsado";
        String estado = linea.getInscripcion().getEstado();
        if ("completada".equals(estado)) return "Ya completaste este curso";
        if ("suspendida".equals(estado)) return "Tu inscripción está suspendida";
        if (!"activa".equals(estado) && !"retirada".equals(estado)) return "La inscripción no está activa";
        if (avance != null && avance.compareTo(AVANCE_MAXIMO) >= 0)
            return "Llevas un avance del " + avance.stripTrailingZeros().toPlainString() + " %; solo se reembolsa con menos del " + AVANCE_MAXIMO + " %";
        return null;
    }

    /** Medio y datos de la devolución según el medio de pago. Lanza 400 con todos los datos que faltan. */
    private static void datosDevolucion(Reembolso r, Pago pago, SolicitudReembolsoRequest req) {
        String medio = req.getMedioDevolucion() == null ? "" : req.getMedioDevolucion().trim().toLowerCase();
        if (AL_MISMO_MEDIO.contains(pago.getMedioPago())) {
            if (!medio.isEmpty() && !"mismo_medio".equals(medio))
                throw new IllegalArgumentException("Este pago se devuelve al mismo medio con que pagaste (" + medioPagoNombre(pago) + ")");
            r.setMedioDevolucion("mismo_medio");
            return;
        }
        List<String> errores = new ArrayList<>();
        switch (medio) {
            case "cuenta_bancaria" -> {
                if (vacio(req.getBanco()) || req.getBanco().trim().length() > 100) errores.add("Escribe el banco");
                String tipo = req.getTipoCuenta() == null ? "" : req.getTipoCuenta().trim().toLowerCase();
                if (!Set.of("ahorros", "corriente").contains(tipo)) errores.add("El tipo de cuenta debe ser ahorros o corriente");
                String numero = soloDigitos(req.getNumeroCuenta());
                if (numero.length() < 6 || numero.length() > 20) errores.add("El número de cuenta debe tener entre 6 y 20 dígitos");
                if (vacio(req.getTitular()) || req.getTitular().trim().length() > 150) errores.add("Escribe el nombre del titular de la cuenta");
                String doc = soloDigitos(req.getDocumentoTitular());
                if (doc.length() < 5 || doc.length() > 15) errores.add("El documento del titular debe tener entre 5 y 15 dígitos");
                if (errores.isEmpty()) {
                    r.setMedioDevolucion("cuenta_bancaria");
                    r.setBanco(req.getBanco().trim()); r.setTipoCuenta(tipo); r.setNumeroCuenta(numero);
                    r.setTitular(req.getTitular().trim()); r.setDocumentoTitular(doc);
                }
            }
            case "nequi" -> {
                String tel = soloDigitos(req.getTelefonoNequi());
                if (!tel.matches("3\\d{9}")) errores.add("El número Nequi debe tener 10 dígitos y empezar por 3");
                else { r.setMedioDevolucion("nequi"); r.setTelefonoNequi(tel); }
            }
            default -> errores.add("Pagaste por " + medioPagoNombre(pago) + ": indica dónde recibir la devolución (cuenta_bancaria o nequi)");
        }
        if (!errores.isEmpty()) throw new IllegalArgumentException(String.join("; ", errores));
    }

    /** El pago queda reembolsado cuando todos sus cursos tienen un reembolso aprobado o devuelto. */
    private void actualizarEstadoPago(Pago pago) {
        boolean todos = pago.getItems().stream().allMatch(it ->
                reembolsoInscripcionRepository.existsByPagoInscripcion_IdAndReembolso_EstadoIn(it.getId(), List.of("aprobado", "devuelto")));
        if (todos) { pago.setEstado("reembolsado"); pagoRepository.save(pago); }
    }

    /** Rechazada o cancelada: los cursos en pausa vuelven al estado que tenían al solicitar. */
    private void restaurarCursos(Reembolso r) {
        r.getItems().forEach(it -> {
            Inscripcion insc = it.getPagoInscripcion().getInscripcion();
            if ("en_reembolso".equals(insc.getEstado())) {
                insc.setEstado(it.getEstadoInscripcionPrevio());
                inscripcionRepository.save(insc);
            }
        });
    }

    /** Nota crédito BIO-NC-AAAA-000001; la fila del consecutivo se bloquea para no repetir números. */
    NotaCredito emitirNota(Reembolso r, Factura factura) {
        int anio = LocalDate.now().getYear();
        ConsecutivoNotaCredito c = consecutivoRepository.bloquear(anio).orElseGet(() -> ConsecutivoNotaCredito.builder().anio(anio).ultimo(0).build());
        c.setUltimo(c.getUltimo() + 1);
        consecutivoRepository.save(c);
        DatosFacturacion datos = datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc().orElse(null);
        NotaCredito n = NotaCredito.builder().reembolso(r).factura(factura).estudiante(r.getPago().getEstudiante()).datosFacturacion(datos)
                .numero(String.format("BIO-NC-%d-%06d", anio, c.getUltimo())).motivo(r.getMotivo()).montoTotal(r.getMonto()).build();
        return notaCreditoRepository.save(n);
    }

    private void enviarNota(NotaCredito n) {
        Reembolso r = n.getReembolso();
        Usuario u = n.getEstudiante().getUsuario();
        StringBuilder filas = new StringBuilder();
        r.getItems().forEach(it -> filas.append("<tr><td style=\"padding:8px;border-bottom:1px solid #E0E2E8\">")
                .append(HtmlUtils.htmlEscape(it.getPagoInscripcion().getInscripcion().getCurso().getNombre()))
                .append("</td><td style=\"padding:8px;border-bottom:1px solid #E0E2E8;text-align:right\">")
                .append(PagoServiceImpl.pesos(it.getMonto())).append("</td></tr>"));
        DatosFacturacion d = n.getDatosFacturacion();
        String devolucion = "devuelto".equals(r.getEstado())
                ? "Devolución: " + HtmlUtils.htmlEscape(destino(r)) + (r.getReferenciaDevolucion() != null ? " · Ref. " + HtmlUtils.htmlEscape(r.getReferenciaDevolucion()) : "")
                : "Te devolveremos el dinero " + HtmlUtils.htmlEscape(aDestino(r)) + "; te avisaremos cuando lo hagamos";
        String pie = (d != null ? HtmlUtils.htmlEscape(d.getRazonSocial() + " · NIT " + d.getNit()) + " · " : "") + devolucion;
        correoService.notaCredito(u.getCorreo(), u.getNombre(), n.getNumero(), n.getFactura().getNumero(), filas.toString(), PagoServiceImpl.pesos(n.getMontoTotal()), pie);
        log.info("Nota crédito {} enviada por correo a {} ({})", n.getNumero(), u.getCorreo(), PagoServiceImpl.pesos(n.getMontoTotal()));
        n.setEnviadaPorCorreo(true);
        n.setEnviadaEn(LocalDateTime.now());
        notaCreditoRepository.save(n);
    }

    private void notificar(Reembolso r, String titulo, String contenido, PerfilContador contador) {
        notificacionRepository.save(Notificacion.builder().usuario(r.getPago().getEstudiante().getUsuario())
                .emisor(contador != null ? contador.getUsuario() : null)
                .tipo("reembolso").entidadId(r.getId()).titulo(titulo).contenido(contenido).build());
    }

    private static void exigirSolicitado(Reembolso r) {
        if (!"solicitado".equals(r.getEstado())) throw new SolicitudInvalidaException("La solicitud de reembolso ya fue resuelta (estado " + r.getEstado() + ")");
    }

    /** Contador activo que puede resolver: administrador financiero o auxiliar (el auditor solo consulta). */
    private PerfilContador contadorQueResuelve(Usuario usuario) {
        PerfilContador c = perfilContador(usuario);
        if (c.getEstado() != EstadoPerfil.activo) throw new SinPermisoException("Tu perfil de contador no está activo");
        if (c.getRolContador() == RolContador.auditor) throw new SinPermisoException("El rol auditor solo consulta los reembolsos");
        return c;
    }

    /** Quien puede hacer la devolución ve la cuenta completa; el auditor (o un perfil inactivo) la ve enmascarada. */
    private Vista vistaContador(Usuario usuario) {
        PerfilContador c = perfilContador(usuario);
        return c.getEstado() == EstadoPerfil.activo && c.getRolContador() != RolContador.auditor ? Vista.COMPLETA : Vista.ENMASCARADA;
    }

    private PerfilContador perfilContador(Usuario usuario) {
        return perfilContadorRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los contadores"));
    }

    private Pago pagoPropio(Integer idPago, Usuario usuario) {
        PerfilEstudiante est = perfilEstudiante(usuario);
        Pago p = pagoRepository.findById(idPago).orElseThrow(() -> new RecursoNoEncontradoException("El pago indicado no existe"));
        if (!p.getEstudiante().getId().equals(est.getId())) throw new SinPermisoException("Este pago no es tuyo");
        return p;
    }

    private PerfilEstudiante perfilEstudiante(Usuario usuario) {
        return perfilEstudianteRepository.findByUsuario_Id(usuario.getId())
                .orElseThrow(() -> new SinPermisoException("Esta función es exclusiva de los estudiantes"));
    }

    private Reembolso buscar(Integer id) {
        return reembolsoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La solicitud de reembolso indicada no existe"));
    }

    private NotaCredito buscarNota(Integer id) {
        return notaCreditoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La nota crédito indicada no existe"));
    }

    private static String medioPagoNombre(Pago p) { return PagoServiceImpl.NOMBRES_MEDIO.getOrDefault(p.getMedioPago(), p.getMedioPago()); }

    /**
     * Destino de la devolución para lo que lee el estudiante (nota crédito, notificaciones y correos): siempre con el
     * número enmascarado (••••7890). El número completo solo lo ve, en la API, el contador que hace la devolución.
     */
    static String destino(Reembolso r) {
        return switch (r.getMedioDevolucion()) {
            case "cuenta_bancaria" -> "la cuenta de " + r.getTipoCuenta() + " " + enmascarar(r.getNumeroCuenta()) + " de " + r.getBanco();
            case "nequi" -> "Nequi " + enmascarar(r.getTelefonoNequi());
            default -> "tu " + medioPagoNombre(r.getPago());
        };
    }

    /** El destino con su preposición: «a la cuenta de ahorros ••••7890 de Bancolombia», «al Nequi ••••4567», «a tu Tarjeta». */
    static String aDestino(Reembolso r) {
        return ("nequi".equals(r.getMedioDevolucion()) ? "al " : "a ") + destino(r);
    }

    /** ••••5678: solo los 4 últimos dígitos. */
    static String enmascarar(String s) {
        if (s == null || s.isBlank()) return s;
        return s.length() <= 4 ? "••••" : "••••" + s.substring(s.length() - 4);
    }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }

    private static String soloDigitos(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }

    private static String textoBusqueda(Reembolso r) {
        Usuario u = r.getPago().getEstudiante().getUsuario();
        return u.getNombre() + " " + u.getApellido() + " " + u.getCorreo() + " "
                + String.join(" ", r.getItems().stream().map(it -> it.getPagoInscripcion().getInscripcion().getCurso().getNombre()).toList());
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

    // ── conversión ──────────────────────────────────────────────────────────

    enum Vista { ESTUDIANTE, COMPLETA, ENMASCARADA }

    private static ReembolsoResponse.Item item(ReembolsoInscripcion it) {
        Curso c = it.getPagoInscripcion().getInscripcion().getCurso();
        return ReembolsoResponse.Item.builder().idInscripcion(it.getPagoInscripcion().getInscripcion().getId()).idCurso(c.getId())
                .curso(c.getNombre()).codigoCurso(c.getCodigo()).monto(it.getMonto()).avance(it.getAvance()).build();
    }

    ReembolsoResponse aReembolso(Reembolso r, Vista vista) {
        Pago p = r.getPago();
        boolean completo = vista == Vista.COMPLETA;
        Optional<Factura> f = facturaRepository.findByPago_Id(p.getId());
        Optional<NotaCredito> n = r.getId() == null ? Optional.empty() : notaCreditoRepository.findByReembolso_Id(r.getId());
        Usuario u = p.getEstudiante().getUsuario();
        boolean conEstudiante = vista != Vista.ESTUDIANTE;
        return ReembolsoResponse.builder()
                .id(r.getId()).idPago(p.getId()).medioPago(p.getMedioPago()).medioPagoNombre(medioPagoNombre(p))
                .referenciaPago(p.getReferenciaExterna()).fechaPago(p.getFechaPago())
                .estado(r.getEstado()).motivo(r.getMotivo()).monto(r.getMonto())
                .items(r.getItems().stream().map(ReembolsoServiceImpl::item).toList())
                .medioDevolucion(r.getMedioDevolucion())
                .medioDevolucionNombre("mismo_medio".equals(r.getMedioDevolucion()) ? "Mismo medio (" + medioPagoNombre(p) + ")" : NOMBRES_DEVOLUCION.get(r.getMedioDevolucion()))
                .banco(r.getBanco()).tipoCuenta(r.getTipoCuenta())
                .numeroCuenta(completo ? r.getNumeroCuenta() : enmascarar(r.getNumeroCuenta()))
                .titular(r.getTitular())
                .documentoTitular(completo ? r.getDocumentoTitular() : enmascarar(r.getDocumentoTitular()))
                .telefonoNequi(completo ? r.getTelefonoNequi() : enmascarar(r.getTelefonoNequi()))
                .solicitadoEn(r.getSolicitadoEn()).resueltoEn(r.getResueltoEn()).justificacion(r.getJustificacion())
                .referenciaDevolucion(r.getReferenciaDevolucion())
                .comprobanteDevolucionUrl(archivoFirma.firmar(r.getComprobanteDevolucionUrl()))
                .devueltoEn(r.getDevueltoEn())
                .idFactura(f.map(Factura::getId).orElse(null)).numeroFactura(f.map(Factura::getNumero).orElse(null))
                .idNotaCredito(n.map(NotaCredito::getId).orElse(null)).numeroNotaCredito(n.map(NotaCredito::getNumero).orElse(null))
                .idEstudiante(conEstudiante ? p.getEstudiante().getId() : null)
                .estudiante(conEstudiante ? u.getNombre() + " " + u.getApellido() : null)
                .correo(conEstudiante ? u.getCorreo() : null)
                .resueltoPor(conEstudiante && r.getContador() != null ? r.getContador().getUsuario().getNombre() + " " + r.getContador().getUsuario().getApellido()
                        : conEstudiante && Boolean.TRUE.equals(r.getAprobacionAutomatica()) ? "Aprobación automática" : null)
                .aprobacionAutomatica(Boolean.TRUE.equals(r.getAprobacionAutomatica()))
                .build();
    }

    private NotaCreditoResponse aNota(NotaCredito n) {
        DatosFacturacion d = n.getDatosFacturacion();
        Usuario u = n.getEstudiante().getUsuario();
        Reembolso r = n.getReembolso();
        return NotaCreditoResponse.builder()
                .id(n.getId()).numero(n.getNumero()).fechaEmision(n.getFechaEmision())
                .razonSocial(d != null ? d.getRazonSocial() : null).nit(d != null ? d.getNit() : null).direccionEmisor(FinanzasServiceImpl.direccionCompleta(d))
                .idFactura(n.getFactura().getId()).numeroFactura(n.getFactura().getNumero()).fechaFactura(n.getFactura().getFechaEmision())
                .idEstudiante(n.getEstudiante().getId()).estudiante(u.getNombre() + " " + u.getApellido()).correo(u.getCorreo()).documento(u.getDocIdentidad())
                .motivo(n.getMotivo())
                .items(r.getItems().stream().map(ReembolsoServiceImpl::item).toList())
                .montoTotal(n.getMontoTotal()).idReembolso(r.getId())
                .medioDevolucionNombre(destino(r))
                .enviadaPorCorreo(n.getEnviadaPorCorreo()).enviadaEn(n.getEnviadaEn())
                .build();
    }
}
