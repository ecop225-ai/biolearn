package co.biolearn.service.impl;

import co.biolearn.dto.HistorialReporteResponse;
import co.biolearn.dto.ReporteFinancieroResponse;
import co.biolearn.dto.ReporteFinancieroResponse.Columna;
import co.biolearn.dto.ReporteFinancieroResponse.Indicador;
import co.biolearn.dto.ReporteFinancieroResponse.Tabla;
import co.biolearn.dto.ReporteRequest;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.ReporteFinancieroService;
import co.biolearn.service.XlsxWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pagos, Fase 3. Reportes financieros por período, en la vista imprimible (PDF) y en Excel:
 * <ul>
 *   <li><b>ingresos</b>: pagos aprobados (por fecha de pago), por medio y por curso, con lo reembolsado de cada pago;</li>
 *   <li><b>facturas</b>: facturas emitidas con sus notas crédito y el saldo;</li>
 *   <li><b>reembolsos</b>: solicitudes del período por estado, con la nota crédito y la devolución;</li>
 *   <li><b>general</b>: indicadores, mes a mes, medios de pago, cursos y estado de los pagos.</li>
 * </ul>
 * Los números de cuenta salen siempre enmascarados. Cada reporte queda registrado con su motivo y su autor.
 */
@Service
@RequiredArgsConstructor
public class ReporteFinancieroServiceImpl implements ReporteFinancieroService {

    static final Map<String, String> TIPOS = Map.of(
            "ingresos", "Reporte de ingresos", "facturas", "Estado de facturas",
            "reembolsos", "Reembolsos", "general", "Reporte financiero general");
    static final int DIAS_MAXIMOS = 366;
    private static final List<String> APROBADOS = List.of("aprobado", "reembolsado");
    private static final Set<String> CON_NOTA = Set.of("aprobado", "devuelto");
    private static final Map<String, String> ESTADOS_REEMBOLSO = new LinkedHashMap<>();
    private static final Map<String, String> ESTADOS_PAGO = new LinkedHashMap<>();
    static {
        ESTADOS_REEMBOLSO.put("solicitado", "En revisión"); ESTADOS_REEMBOLSO.put("aprobado", "Por devolver");
        ESTADOS_REEMBOLSO.put("devuelto", "Devuelto"); ESTADOS_REEMBOLSO.put("rechazado", "Rechazado");
        ESTADOS_REEMBOLSO.put("cancelado", "Cancelado");
        ESTADOS_PAGO.put("aprobado", "Aprobado"); ESTADOS_PAGO.put("reembolsado", "Reembolsado");
        ESTADOS_PAGO.put("pendiente", "Pendiente"); ESTADOS_PAGO.put("rechazado", "Rechazado");
        ESTADOS_PAGO.put("cancelado", "Cancelado");
    }

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final NotaCreditoRepository notaCreditoRepository;
    private final DatosFacturacionRepository datosFacturacionRepository;
    private final ReporteFinancieroRepository reporteRepository;

    // ── API ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReporteFinancieroResponse generar(ReporteRequest req, Usuario usuario) {
        return construir(req, usuario, "pdf");
    }

    @Override
    @Transactional
    public byte[] excel(ReporteRequest req, Usuario usuario) {
        return aExcel(construir(req, usuario, "excel"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialReporteResponse> historial() {
        return reporteRepository.findTop50ByOrderByGeneradoEnDescIdDesc().stream().map(r -> HistorialReporteResponse.builder()
                .id(r.getId()).tipo(r.getTipo()).tipoNombre(TIPOS.get(r.getTipo())).desde(r.getDesde()).hasta(r.getHasta())
                .formato(r.getFormato()).motivo(r.getMotivo()).generadoPor(FinanzasServiceImpl.nombre(r.getUsuario()))
                .generadoEn(r.getGeneradoEn()).build()).toList();
    }

    // ── construcción ────────────────────────────────────────────────────────

    ReporteFinancieroResponse construir(ReporteRequest req, Usuario usuario, String formato) {
        validarPeriodo(req.getDesde(), req.getHasta());
        LocalDateTime desde = req.getDesde().atStartOfDay();
        LocalDateTime hasta = req.getHasta().plusDays(1).atStartOfDay().minusNanos(1);
        ReporteFinanciero registro = reporteRepository.save(ReporteFinanciero.builder().tipo(req.getTipo())
                .desde(req.getDesde()).hasta(req.getHasta()).formato(formato).motivo(req.getMotivo().trim()).usuario(usuario).build());

        ReporteFinancieroResponse r = ReporteFinancieroResponse.builder().idRegistro(registro.getId()).tipo(req.getTipo())
                .titulo(TIPOS.get(req.getTipo())).desde(req.getDesde()).hasta(req.getHasta())
                .generadoEn(registro.getGeneradoEn() != null ? registro.getGeneradoEn() : LocalDateTime.now())
                .generadoPor(FinanzasServiceImpl.nombre(usuario)).motivo(registro.getMotivo())
                .indicadores(new ArrayList<>()).tablas(new ArrayList<>()).build();
        datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc().ifPresent(d -> { r.setRazonSocial(d.getRazonSocial()); r.setNit(d.getNit()); });

        switch (req.getTipo()) {
            case "ingresos" -> ingresos(r, desde, hasta);
            case "facturas" -> facturas(r, desde, hasta);
            case "reembolsos" -> reembolsos(r, desde, hasta);
            case "general" -> general(r, desde, hasta);
            default -> throw new IllegalArgumentException("El tipo de reporte debe ser ingresos, facturas, reembolsos o general");
        }
        return r;
    }

    static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        if (ChronoUnit.DAYS.between(desde, hasta) >= DIAS_MAXIMOS)
            throw new IllegalArgumentException("El período máximo de un reporte es de un año (366 días)");
    }

    private void ingresos(ReporteFinancieroResponse r, LocalDateTime desde, LocalDateTime hasta) {
        List<Pago> pagos = pagoRepository.findByEstadoInAndFechaPagoBetweenOrderByFechaPagoAsc(APROBADOS, desde, hasta);
        Map<Integer, BigDecimal> reembolsado = reembolsadoPorPago(pagos);
        BigDecimal bruto = suma(pagos, Pago::getMonto);
        BigDecimal devuelto = reembolsado.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        r.getIndicadores().add(ind("Ingresos brutos", bruto, "moneda"));
        r.getIndicadores().add(ind("Reembolsado (notas crédito)", devuelto, "moneda"));
        r.getIndicadores().add(ind("Ingresos netos", bruto.subtract(devuelto), "moneda"));
        r.getIndicadores().add(ind("Pagos aprobados", (long) pagos.size(), "numero"));
        r.getIndicadores().add(ind("Valor promedio por pago", pagos.isEmpty() ? BigDecimal.ZERO
                : bruto.divide(BigDecimal.valueOf(pagos.size()), 0, RoundingMode.HALF_UP), "moneda"));

        r.getTablas().add(porMedio(pagos, bruto));
        r.getTablas().add(porCurso(pagos));

        List<List<Object>> filas = new ArrayList<>();
        for (Pago p : pagos) {
            BigDecimal reem = reembolsado.getOrDefault(p.getId(), BigDecimal.ZERO);
            filas.add(fila(p.getFechaPago(), "#" + p.getId(), numeroFactura(p), estudiante(p.getEstudiante()), correo(p.getEstudiante()),
                    cursos(p), medio(p), p.getMonto(), reem, p.getMonto().subtract(reem)));
        }
        BigDecimal netoTotal = bruto.subtract(devuelto);
        r.getTablas().add(Tabla.builder().titulo("Pagos aprobados").detalle(true)
                .columnas(cols("Fecha de pago:fecha", "Pago:texto", "Factura:texto", "Estudiante:texto", "Correo:texto",
                        "Cursos:texto", "Medio:texto", "Monto:moneda", "Reembolsado:moneda", "Neto:moneda"))
                .filas(filas).total(fila("Total", null, null, null, null, null, (long) pagos.size() + " pagos", bruto, devuelto, netoTotal))
                .nota("Pagos aprobados con fecha de pago dentro del período. «Reembolsado» suma las notas crédito emitidas sobre cada pago.").build());
    }

    private void facturas(ReporteFinancieroResponse r, LocalDateTime desde, LocalDateTime hasta) {
        List<Factura> facturas = facturaRepository.findByFechaEmisionBetweenOrderByFechaEmisionAsc(desde, hasta);
        BigDecimal total = BigDecimal.ZERO, notas = BigDecimal.ZERO;
        int conNota = 0;
        List<List<Object>> filas = new ArrayList<>();
        for (Factura f : facturas) {
            List<NotaCredito> ns = notaCreditoRepository.findByFactura_IdOrderByFechaEmisionAsc(f.getId());
            BigDecimal valorNotas = suma(ns, NotaCredito::getMontoTotal);
            total = total.add(f.getMontoTotal());
            notas = notas.add(valorNotas);
            if (!ns.isEmpty()) conNota++;
            filas.add(fila(f.getFechaEmision(), f.getNumero(), estudiante(f.getEstudiante()), f.getDescripcion(),
                    f.getMontoTotal(), ns.isEmpty() ? "—" : ns.stream().map(NotaCredito::getNumero).collect(Collectors.joining(", ")),
                    valorNotas, f.getMontoTotal().subtract(valorNotas),
                    valorNotas.signum() == 0 ? "Vigente" : valorNotas.compareTo(f.getMontoTotal()) >= 0 ? "Corregida por completo" : "Corregida en parte"));
        }
        r.getIndicadores().add(ind("Facturas emitidas", (long) facturas.size(), "numero"));
        r.getIndicadores().add(ind("Total facturado", total, "moneda"));
        r.getIndicadores().add(ind("Facturas con nota crédito", (long) conNota, "numero"));
        r.getIndicadores().add(ind("Valor de las notas crédito", notas, "moneda"));
        r.getIndicadores().add(ind("Saldo facturado", total.subtract(notas), "moneda"));
        r.getTablas().add(Tabla.builder().titulo("Facturas emitidas").detalle(true)
                .columnas(cols("Fecha de emisión:fecha", "Factura:texto", "Estudiante:texto", "Descripción:texto", "Total:moneda",
                        "Notas crédito:texto", "Valor notas:moneda", "Saldo:moneda", "Estado:texto"))
                .filas(filas).total(fila("Total", (long) facturas.size() + " facturas", null, null, total, null, notas, total.subtract(notas), null))
                .nota("Facturas emitidas dentro del período; las notas crédito pueden ser de fechas posteriores.").build());
    }

    private void reembolsos(ReporteFinancieroResponse r, LocalDateTime desde, LocalDateTime hasta) {
        List<Reembolso> rs = reembolsoRepository.findBySolicitadoEnBetweenOrderBySolicitadoEnAsc(desde, hasta);
        BigDecimal solicitado = suma(rs, Reembolso::getMonto);
        BigDecimal devuelto = suma(rs.stream().filter(x -> "devuelto".equals(x.getEstado())).toList(), Reembolso::getMonto);
        BigDecimal porDevolver = suma(rs.stream().filter(x -> "aprobado".equals(x.getEstado())).toList(), Reembolso::getMonto);
        long automaticas = rs.stream().filter(x -> Boolean.TRUE.equals(x.getAprobacionAutomatica())).count();

        r.getIndicadores().add(ind("Solicitudes", (long) rs.size(), "numero"));
        r.getIndicadores().add(ind("Monto solicitado", solicitado, "moneda"));
        r.getIndicadores().add(ind("Devuelto", devuelto, "moneda"));
        r.getIndicadores().add(ind("Por devolver", porDevolver, "moneda"));
        r.getIndicadores().add(ind("Aprobadas automáticamente", automaticas, "numero"));

        List<List<Object>> porEstado = new ArrayList<>();
        ESTADOS_REEMBOLSO.forEach((k, nombre) -> {
            List<Reembolso> de = rs.stream().filter(x -> k.equals(x.getEstado())).toList();
            porEstado.add(fila(nombre, (long) de.size(), suma(de, Reembolso::getMonto)));
        });
        r.getTablas().add(Tabla.builder().titulo("Por estado").columnas(cols("Estado:texto", "Solicitudes:numero", "Monto:moneda"))
                .filas(porEstado).total(fila("Total", (long) rs.size(), solicitado)).build());

        List<List<Object>> filas = new ArrayList<>();
        for (Reembolso x : rs) {
            NotaCredito nota = notaCreditoRepository.findByReembolso_Id(x.getId()).orElse(null);
            filas.add(fila(x.getSolicitadoEn(), "#" + x.getId(), estudiante(x.getPago().getEstudiante()), "#" + x.getPago().getId() + " · " + medio(x.getPago()),
                    x.getItems().stream().map(i -> i.getPagoInscripcion().getInscripcion().getCurso().getNombre()).collect(Collectors.joining(", ")),
                    x.getMonto(), ESTADOS_REEMBOLSO.getOrDefault(x.getEstado(), x.getEstado()),
                    Boolean.TRUE.equals(x.getAprobacionAutomatica()) ? "Automática" : x.getContador() != null ? "Contador" : "—",
                    destino(x), nota == null ? "—" : nota.getNumero(), x.getDevueltoEn(), vacio(x.getReferenciaDevolucion()) ? "—" : x.getReferenciaDevolucion()));
        }
        r.getTablas().add(Tabla.builder().titulo("Solicitudes de reembolso").detalle(true)
                .columnas(cols("Solicitado:fecha", "Reembolso:texto", "Estudiante:texto", "Pago:texto", "Cursos:texto", "Monto:moneda",
                        "Estado:texto", "Aprobación:texto", "Destino:texto", "Nota crédito:texto", "Devuelto:fecha", "Referencia:texto"))
                .filas(filas).total(fila("Total", (long) rs.size() + " solicitudes", null, null, null, solicitado, null, null, null, null, null, null))
                .nota("Solicitudes hechas dentro del período, en cualquier estado. Las cuentas y los Nequi se muestran enmascarados.").build());
    }

    private void general(ReporteFinancieroResponse r, LocalDateTime desde, LocalDateTime hasta) {
        List<Pago> aprobados = pagoRepository.findByEstadoInAndFechaPagoBetweenOrderByFechaPagoAsc(APROBADOS, desde, hasta);
        List<Reembolso> devueltos = reembolsoRepository.findByEstadoAndDevueltoEnBetween("devuelto", desde, hasta);
        List<Factura> facturas = facturaRepository.findByFechaEmisionBetweenOrderByFechaEmisionAsc(desde, hasta);
        List<Pago> creados = pagoRepository.findAll().stream()
                .filter(p -> p.getCreadoEn() != null && !p.getCreadoEn().isBefore(desde) && !p.getCreadoEn().isAfter(hasta)).toList();
        List<NotaCredito> notas = notaCreditoRepository.findAll().stream()
                .filter(n -> n.getFechaEmision() != null && !n.getFechaEmision().isBefore(desde) && !n.getFechaEmision().isAfter(hasta)).toList();
        BigDecimal bruto = suma(aprobados, Pago::getMonto);
        BigDecimal devuelto = suma(devueltos, Reembolso::getMonto);
        long porConfirmar = creados.stream().filter(p -> "pendiente".equals(p.getEstado()) && "transferencia".equals(p.getMedioPago())).count();

        r.getIndicadores().add(ind("Ingresos brutos", bruto, "moneda"));
        r.getIndicadores().add(ind("Dinero devuelto", devuelto, "moneda"));
        r.getIndicadores().add(ind("Ingresos netos", bruto.subtract(devuelto), "moneda"));
        r.getIndicadores().add(ind("Facturas emitidas", (long) facturas.size(), "numero"));
        r.getIndicadores().add(ind("Notas crédito emitidas", (long) notas.size(), "numero"));
        r.getIndicadores().add(ind("Transferencias por confirmar", porConfirmar, "numero"));

        // Mes a mes
        Map<YearMonth, BigDecimal[]> meses = new TreeMap<>();
        for (YearMonth m = YearMonth.from(desde); !m.isAfter(YearMonth.from(hasta)); m = m.plusMonths(1))
            meses.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        aprobados.forEach(p -> meses.computeIfPresent(YearMonth.from(p.getFechaPago()), (k, v) -> { v[0] = v[0].add(BigDecimal.ONE); v[1] = v[1].add(p.getMonto()); return v; }));
        devueltos.forEach(x -> meses.computeIfPresent(YearMonth.from(x.getDevueltoEn()), (k, v) -> { v[2] = v[2].add(x.getMonto()); return v; }));
        DateTimeFormatter fm = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"));
        List<List<Object>> filasMes = new ArrayList<>();
        meses.forEach((m, v) -> filasMes.add(fila(capital(m.format(fm)), v[0].longValue(), v[1], v[2], v[1].subtract(v[2]))));
        r.getTablas().add(Tabla.builder().titulo("Mes a mes").columnas(cols("Mes:texto", "Pagos:numero", "Ingresos:moneda", "Devuelto:moneda", "Neto:moneda"))
                .filas(filasMes).total(fila("Total", (long) aprobados.size(), bruto, devuelto, bruto.subtract(devuelto))).build());

        r.getTablas().add(porMedio(aprobados, bruto));

        List<List<Object>> porEstado = new ArrayList<>();
        ESTADOS_PAGO.forEach((k, nombre) -> {
            List<Pago> de = creados.stream().filter(p -> k.equals(p.getEstado())).toList();
            porEstado.add(fila(nombre, (long) de.size(), suma(de, Pago::getMonto)));
        });
        r.getTablas().add(Tabla.builder().titulo("Pagos creados en el período por estado")
                .columnas(cols("Estado:texto", "Pagos:numero", "Monto:moneda")).filas(porEstado)
                .total(fila("Total", (long) creados.size(), suma(creados, Pago::getMonto))).build());

        Tabla cursos = porCurso(aprobados);
        cursos.setTitulo("Cursos con más ingresos");
        if (cursos.getFilas().size() > 10) cursos.setFilas(new ArrayList<>(cursos.getFilas().subList(0, 10)));
        cursos.setTotal(null);
        cursos.setNota("Los 10 cursos con más ingresos del período.");
        r.getTablas().add(cursos);
    }

    // ── tablas comunes ──────────────────────────────────────────────────────

    private Tabla porMedio(List<Pago> pagos, BigDecimal bruto) {
        Map<String, List<Pago>> por = pagos.stream().collect(Collectors.groupingBy(this::medio, TreeMap::new, Collectors.toList()));
        List<List<Object>> filas = new ArrayList<>();
        por.forEach((m, ps) -> {
            BigDecimal monto = suma(ps, Pago::getMonto);
            filas.add(fila(m, (long) ps.size(), monto, porcentaje(monto, bruto)));
        });
        return Tabla.builder().titulo("Por medio de pago").columnas(cols("Medio:texto", "Pagos:numero", "Monto:moneda", "Participación:porcentaje"))
                .filas(filas).total(fila("Total", (long) pagos.size(), bruto, bruto.signum() == 0 ? BigDecimal.ZERO : BigDecimal.ONE)).build();
    }

    private Tabla porCurso(List<Pago> pagos) {
        Map<String, BigDecimal[]> por = new HashMap<>();
        for (Pago p : pagos)
            for (PagoInscripcion it : p.getItems()) {
                String c = it.getInscripcion().getCurso().getNombre();
                BigDecimal[] v = por.computeIfAbsent(c, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                v[0] = v[0].add(BigDecimal.ONE);
                v[1] = v[1].add(it.getMontoAsignado());
            }
        List<List<Object>> filas = por.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<String, BigDecimal[]> e) -> e.getValue()[1]).reversed().thenComparing(Map.Entry::getKey))
                .map(e -> fila(e.getKey(), e.getValue()[0].longValue(), e.getValue()[1])).collect(Collectors.toCollection(ArrayList::new));
        return Tabla.builder().titulo("Por curso").columnas(cols("Curso:texto", "Inscripciones pagadas:numero", "Monto:moneda"))
                .filas(filas).total(fila("Total", filas.stream().mapToLong(f -> (Long) f.get(1)).sum(),
                        filas.stream().map(f -> (BigDecimal) f.get(2)).reduce(BigDecimal.ZERO, BigDecimal::add))).build();
    }

    // ── Excel ───────────────────────────────────────────────────────────────

    static byte[] aExcel(ReporteFinancieroResponse r) {
        XlsxWriter x = new XlsxWriter();
        XlsxWriter.Hoja h = x.hoja("Resumen", 36, 22, 20, 20, 16);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        h.titulo(r.getTitulo() + (r.getRazonSocial() == null ? "" : " — " + r.getRazonSocial()));
        h.nota((r.getNit() == null ? "" : "NIT " + r.getNit() + " · ") + "Período: " + r.getDesde().format(f) + " a " + r.getHasta().format(f));
        h.nota("Generado por " + r.getGeneradoPor() + " el " + r.getGeneradoEn().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + " · Motivo: " + r.getMotivo());
        h.vacia().subtitulo("Indicadores");
        for (Indicador i : r.getIndicadores()) h.par(i.getEtiqueta(), i.getValor(), tipo(i.getFormato()));
        for (Tabla t : r.getTablas()) {
            if (t.isDetalle()) continue;
            h.vacia().subtitulo(t.getTitulo());
            escribirTabla(h, t);
        }
        for (Tabla t : r.getTablas()) {
            if (!t.isDetalle()) continue;
            XlsxWriter.Hoja d = x.hoja(t.getTitulo(), anchos(t));
            d.titulo(t.getTitulo() + " — " + r.getDesde().format(f) + " a " + r.getHasta().format(f));
            escribirTabla(d, t);
        }
        return x.bytes();
    }

    private static void escribirTabla(XlsxWriter.Hoja h, Tabla t) {
        List<XlsxWriter.Tipo> tipos = t.getColumnas().stream().map(c -> tipo(c.getTipo())).toList();
        h.encabezado(t.getColumnas().stream().map(Columna::getNombre).toList());
        t.getFilas().forEach(fl -> h.fila(fl, tipos));
        if (t.getFilas().isEmpty()) h.nota("Sin movimientos en el período.");
        if (t.getTotal() != null) h.total(t.getTotal(), tipos);
        if (t.getNota() != null) h.nota(t.getNota());
    }

    private static double[] anchos(Tabla t) {
        double[] a = new double[t.getColumnas().size()];
        for (int i = 0; i < a.length; i++) {
            Columna c = t.getColumnas().get(i);
            int max = c.getNombre().length();
            for (List<Object> fl : t.getFilas()) {
                Object v = fl.get(i);
                if (v instanceof String s) max = Math.max(max, s.length());
            }
            a[i] = switch (c.getTipo()) {
                case "fecha" -> 17;
                case "moneda" -> 15;
                case "numero", "porcentaje" -> Math.max(11, max + 2);
                default -> Math.min(48, Math.max(12, max + 2));
            };
        }
        return a;
    }

    private static XlsxWriter.Tipo tipo(String t) {
        return switch (t == null ? "texto" : t) {
            case "moneda" -> XlsxWriter.Tipo.MONEDA;
            case "numero" -> XlsxWriter.Tipo.NUMERO;
            case "porcentaje" -> XlsxWriter.Tipo.PORCENTAJE;
            case "fecha" -> XlsxWriter.Tipo.FECHA;
            default -> XlsxWriter.Tipo.TEXTO;
        };
    }

    // ── utilidades ──────────────────────────────────────────────────────────

    private Map<Integer, BigDecimal> reembolsadoPorPago(List<Pago> pagos) {
        Map<Integer, BigDecimal> out = new HashMap<>();
        for (Pago p : pagos)
            for (Reembolso x : reembolsoRepository.findByPago_Id(p.getId()))
                if (CON_NOTA.contains(x.getEstado())) out.merge(p.getId(), x.getMonto(), BigDecimal::add);
        return out;
    }

    private String numeroFactura(Pago p) { return facturaRepository.findByPago_Id(p.getId()).map(Factura::getNumero).orElse("—"); }

    private String medio(Pago p) { return PagoServiceImpl.NOMBRES_MEDIO.getOrDefault(p.getMedioPago(), p.getMedioPago()); }

    private static String cursos(Pago p) {
        return p.getItems().stream().map(i -> i.getInscripcion().getCurso().getNombre()).collect(Collectors.joining(", "));
    }

    private static String estudiante(PerfilEstudiante e) { return e == null ? "—" : FinanzasServiceImpl.nombre(e.getUsuario()); }

    private static String correo(PerfilEstudiante e) { return e == null ? "—" : e.getUsuario().getCorreo(); }

    /** Destino de la devolución, siempre enmascarado. */
    String destino(Reembolso r) {
        return switch (r.getMedioDevolucion()) {
            case "cuenta_bancaria" -> "Cuenta de " + r.getTipoCuenta() + " " + ReembolsoServiceImpl.enmascarar(r.getNumeroCuenta()) + " · " + r.getBanco();
            case "nequi" -> "Nequi " + ReembolsoServiceImpl.enmascarar(r.getTelefonoNequi());
            default -> "Mismo medio (" + medio(r.getPago()) + ")";
        };
    }

    private static <T> BigDecimal suma(Collection<T> xs, Function<T, BigDecimal> f) {
        return xs.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO : parte.divide(total, 4, RoundingMode.HALF_UP);
    }

    private static Indicador ind(String etiqueta, Object valor, String formato) {
        return Indicador.builder().etiqueta(etiqueta).valor(valor).formato(formato).build();
    }

    /** Columnas «Nombre:tipo». */
    private static List<Columna> cols(String... defs) {
        return Arrays.stream(defs).map(d -> {
            int i = d.lastIndexOf(':');
            return Columna.builder().nombre(d.substring(0, i)).tipo(d.substring(i + 1)).build();
        }).toList();
    }

    private static List<Object> fila(Object... vs) { return new ArrayList<>(Arrays.asList(vs)); }

    private static String capital(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    private static boolean vacio(String s) { return s == null || s.isBlank(); }
}
