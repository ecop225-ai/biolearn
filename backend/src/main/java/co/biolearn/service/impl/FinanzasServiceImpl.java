package co.biolearn.service.impl;

import co.biolearn.dto.*;
import co.biolearn.enums.EstadoPerfil;
import co.biolearn.enums.RolContador;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.SinPermisoException;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.*;
import co.biolearn.repository.*;
import co.biolearn.service.FinanzasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Stream;

/**
 * Pagos, Fase 3. Datos del emisor de las facturas y de la cuenta de recaudo: cada cambio guarda una versión nueva,
 * así las facturas y notas crédito anteriores conservan los datos con que se emitieron. Editan el Administrador y
 * el contador administrador financiero; el auxiliar y el auditor consultan.
 * También arma el inicio del contador con cifras reales.
 */
@Service
@RequiredArgsConstructor
public class FinanzasServiceImpl implements FinanzasService {

    static final String SIN_PERMISO = "Solo el Administrador y el administrador financiero pueden cambiar estos datos";

    private final DatosFacturacionRepository datosFacturacionRepository;
    private final DatosRecaudoRepository datosRecaudoRepository;
    private final PerfilContadorRepository perfilContadorRepository;
    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;

    // ── configuración ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionFinancieraResponse configuracion(Usuario usuario) {
        return ConfiguracionFinancieraResponse.builder()
                .facturacion(datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc().map(d -> aFacturacion(d, true)).orElse(null))
                .recaudo(datosRecaudoRepository.findFirstByOrderByActualizadoEnDescIdDesc().map(d -> aRecaudo(d, true)).orElse(null))
                .puedeEditar(puedeEditar(usuario)).build();
    }

    @Override
    @Transactional
    public DatosFacturacionResponse actualizarFacturacion(DatosFacturacionRequest req, Usuario usuario) {
        exigirEditor(usuario);
        DatosFacturacion nuevo = DatosFacturacion.builder()
                .razonSocial(req.getRazonSocial().trim()).nit(req.getNit().trim())
                .direccion(limpio(req.getDireccion())).ciudad(limpio(req.getCiudad()))
                .correo(limpio(req.getCorreo())).telefono(limpio(req.getTelefono()))
                .actualizadoPor(usuario).build();
        datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc().ifPresent(v -> {
            nuevo.setLogoUrl(v.getLogoUrl());
            if (iguales(v, nuevo)) throw new SolicitudInvalidaException("No hay cambios para guardar");
        });
        return aFacturacion(datosFacturacionRepository.save(nuevo), true);
    }

    @Override
    @Transactional
    public DatosRecaudoResponse actualizarRecaudo(DatosRecaudoRequest req, Usuario usuario) {
        exigirEditor(usuario);
        DatosRecaudo nuevo = DatosRecaudo.builder()
                .banco(req.getBanco().trim()).tipoCuenta(req.getTipoCuenta()).numeroCuenta(req.getNumeroCuenta().trim())
                .titular(req.getTitular().trim()).documentoTitular(req.getDocumentoTitular().trim())
                .telefonoNequi(limpio(req.getTelefonoNequi())).instrucciones(limpio(req.getInstrucciones()))
                .actualizadoPor(usuario).build();
        datosRecaudoRepository.findFirstByOrderByActualizadoEnDescIdDesc().ifPresent(v -> {
            if (iguales(v, nuevo)) throw new SolicitudInvalidaException("No hay cambios para guardar");
        });
        return aRecaudo(datosRecaudoRepository.save(nuevo), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatosFacturacionResponse> historialFacturacion() {
        List<DatosFacturacion> todas = datosFacturacionRepository.findAllByOrderByActualizadoEnDescIdDesc();
        List<DatosFacturacionResponse> out = new ArrayList<>();
        for (int i = 0; i < todas.size(); i++) out.add(aFacturacion(todas.get(i), i == 0));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatosRecaudoResponse> historialRecaudo() {
        List<DatosRecaudo> todas = datosRecaudoRepository.findAllByOrderByActualizadoEnDescIdDesc();
        List<DatosRecaudoResponse> out = new ArrayList<>();
        for (int i = 0; i < todas.size(); i++) out.add(aRecaudo(todas.get(i), i == 0));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public DatosRecaudoResponse recaudoVigente() {
        DatosRecaudoResponse r = datosRecaudoRepository.findFirstByOrderByActualizadoEnDescIdDesc().map(d -> aRecaudo(d, true))
                .orElseThrow(() -> new co.biolearn.exception.RecursoNoEncontradoException("Todavía no hay una cuenta para transferencias; paga en línea o comunícate con BioLearn"));
        r.setActualizadoPor(null);                                      // el estudiante no necesita saber quién la cambió
        return r;
    }

    // ── inicio del contador ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResumenFinancieroResponse resumen() {
        LocalDate hoy = LocalDate.now();
        YearMonth mes = YearMonth.from(hoy);
        LocalDateTime inicio6 = mes.minusMonths(5).atDay(1).atStartOfDay();
        LocalDateTime fin = hoy.plusDays(1).atStartOfDay().minusNanos(1);

        List<Pago> pagos = pagoRepository.findByEstadoInAndFechaPagoBetweenOrderByFechaPagoAsc(List.of("aprobado", "reembolsado"), inicio6, fin);
        List<Reembolso> devueltos = reembolsoRepository.findByEstadoAndDevueltoEnBetween("devuelto", inicio6, fin);

        Map<YearMonth, BigDecimal[]> porMes = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) porMes.put(mes.minusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        pagos.forEach(p -> porMes.computeIfPresent(YearMonth.from(p.getFechaPago()), (k, v) -> { v[0] = v[0].add(p.getMonto()); return v; }));
        devueltos.forEach(r -> porMes.computeIfPresent(YearMonth.from(r.getDevueltoEn()), (k, v) -> { v[1] = v[1].add(r.getMonto()); return v; }));

        BigDecimal[] actual = porMes.get(mes);
        long pagosMes = pagos.stream().filter(p -> YearMonth.from(p.getFechaPago()).equals(mes)).count();

        long transferencias = pagoRepository.findByEstadoOrderByCreadoEnDesc("pendiente").stream()
                .filter(p -> "transferencia".equals(p.getMedioPago()) && p.getComprobanteUrl() != null).count();
        List<Reembolso> solicitados = reembolsoRepository.findByEstado("solicitado");
        List<Reembolso> aprobados = reembolsoRepository.findByEstado("aprobado");

        // Últimos movimientos: pagos aprobados y reembolsos recientes, del más nuevo al más antiguo
        List<ResumenFinancieroResponse.Movimiento> movs = Stream.concat(
                pagos.stream().map(p -> ResumenFinancieroResponse.Movimiento.builder().fecha(p.getFechaPago()).tipo("pago").id(p.getId())
                        .descripcion("Pago #" + p.getId() + " · " + PagoServiceImpl.NOMBRES_MEDIO.getOrDefault(p.getMedioPago(), p.getMedioPago()))
                        .estudiante(nombre(p.getEstudiante().getUsuario())).monto(p.getMonto()).estado(p.getEstado()).build()),
                reembolsoRepository.findBySolicitadoEnBetweenOrderBySolicitadoEnAsc(inicio6, fin).stream()
                        .map(r -> ResumenFinancieroResponse.Movimiento.builder()
                                .fecha(r.getDevueltoEn() != null ? r.getDevueltoEn() : r.getResueltoEn() != null ? r.getResueltoEn() : r.getSolicitadoEn())
                                .tipo("reembolso").id(r.getId()).descripcion("Reembolso #" + r.getId() + " del pago #" + r.getPago().getId())
                                .estudiante(nombre(r.getSolicitante())).monto(r.getMonto()).estado(r.getEstado()).build()))
                .sorted(Comparator.comparing(ResumenFinancieroResponse.Movimiento::getFecha).reversed())
                .limit(8).toList();

        return ResumenFinancieroResponse.builder()
                .ingresosMes(actual[0]).pagosMes(pagosMes).reembolsadoMes(actual[1]).netoMes(actual[0].subtract(actual[1]))
                .transferenciasPorConfirmar(transferencias)
                .reembolsosPorRevisar(solicitados.size())
                .reembolsosPorDevolver(aprobados.size())
                .montoPorDevolver(aprobados.stream().map(Reembolso::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add))
                .ultimosMeses(porMes.entrySet().stream().map(e -> ResumenFinancieroResponse.Mes.builder()
                        .mes(e.getKey().toString()).ingresos(e.getValue()[0]).reembolsos(e.getValue()[1]).build()).toList())
                .movimientos(movs).build();
    }

    // ── permisos ────────────────────────────────────────────────────────────

    boolean puedeEditar(Usuario u) {
        if (u.getTipoUsuario() == RolUsuario.admin) return true;
        if (u.getTipoUsuario() != RolUsuario.contador) return false;
        return perfilContadorRepository.findByUsuario_Id(u.getId())
                .map(c -> c.getEstado() == EstadoPerfil.activo && c.getRolContador() == RolContador.administrador_financiero)
                .orElse(false);
    }

    private void exigirEditor(Usuario u) {
        if (!puedeEditar(u)) throw new SinPermisoException(SIN_PERMISO);
    }

    // ── conversión ──────────────────────────────────────────────────────────

    static DatosFacturacionResponse aFacturacion(DatosFacturacion d, boolean vigente) {
        return DatosFacturacionResponse.builder().id(d.getId()).razonSocial(d.getRazonSocial()).nit(d.getNit())
                .direccion(d.getDireccion()).ciudad(d.getCiudad()).correo(d.getCorreo()).telefono(d.getTelefono())
                .actualizadoEn(d.getActualizadoEn()).actualizadoPor(d.getActualizadoPor() == null ? null : nombre(d.getActualizadoPor()))
                .vigente(vigente).build();
    }

    static DatosRecaudoResponse aRecaudo(DatosRecaudo d, boolean vigente) {
        return DatosRecaudoResponse.builder().id(d.getId()).banco(d.getBanco()).tipoCuenta(d.getTipoCuenta())
                .numeroCuenta(d.getNumeroCuenta()).titular(d.getTitular()).documentoTitular(d.getDocumentoTitular())
                .telefonoNequi(d.getTelefonoNequi()).instrucciones(d.getInstrucciones())
                .actualizadoEn(d.getActualizadoEn()).actualizadoPor(d.getActualizadoPor() == null ? null : nombre(d.getActualizadoPor()))
                .vigente(vigente).build();
    }

    private static boolean iguales(DatosFacturacion a, DatosFacturacion b) {
        return Objects.equals(a.getRazonSocial(), b.getRazonSocial()) && Objects.equals(a.getNit(), b.getNit())
                && Objects.equals(a.getDireccion(), b.getDireccion()) && Objects.equals(a.getCiudad(), b.getCiudad())
                && Objects.equals(a.getCorreo(), b.getCorreo()) && Objects.equals(a.getTelefono(), b.getTelefono());
    }

    private static boolean iguales(DatosRecaudo a, DatosRecaudo b) {
        return Objects.equals(a.getBanco(), b.getBanco()) && Objects.equals(a.getTipoCuenta(), b.getTipoCuenta())
                && Objects.equals(a.getNumeroCuenta(), b.getNumeroCuenta()) && Objects.equals(a.getTitular(), b.getTitular())
                && Objects.equals(a.getDocumentoTitular(), b.getDocumentoTitular()) && Objects.equals(a.getTelefonoNequi(), b.getTelefonoNequi())
                && Objects.equals(a.getInstrucciones(), b.getInstrucciones());
    }

    /** Dirección del emisor con la ciudad, para facturas y notas crédito («Calle 10 # 20-30, Medellín»). */
    static String direccionCompleta(DatosFacturacion d) {
        if (d == null) return null;
        String dir = d.getDireccion(), ciudad = d.getCiudad();
        if (ciudad == null || ciudad.isBlank() || (dir != null && dir.toLowerCase().contains(ciudad.toLowerCase()))) return dir;
        return dir == null || dir.isBlank() ? ciudad : dir + ", " + ciudad;
    }

    private static String limpio(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    static String nombre(Usuario u) { return u.getNombre() + " " + u.getApellido(); }
}
