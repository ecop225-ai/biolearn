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
@DisplayName("FinanzasServiceImpl — datos de facturación y recaudo, e inicio del contador (Pagos, Fase 3)")
class FinanzasServiceImplTest {

    @Mock private DatosFacturacionRepository datosFacturacionRepository;
    @Mock private DatosRecaudoRepository datosRecaudoRepository;
    @Mock private PerfilContadorRepository perfilContadorRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;

    @InjectMocks private FinanzasServiceImpl servicio;

    private Usuario admin, financiero, auxiliar, auditor, estudianteU;
    private DatosFacturacion vigente;
    private DatosRecaudo cuenta;

    private static Usuario usuario(int id, RolUsuario rol) {
        return Usuario.builder().id(id).nombre("N" + id).apellido("A").correo(id + "@t.co").tipoUsuario(rol).estado(EstadoUsuario.activo).build();
    }

    private void contador(Usuario u, RolContador rol) {
        when(perfilContadorRepository.findByUsuario_Id(u.getId())).thenReturn(Optional.of(
                PerfilContador.builder().id(u.getId()).usuario(u).rolContador(rol).estado(EstadoPerfil.activo).build()));
    }

    private static DatosFacturacionRequest facturacion(String razon) {
        return DatosFacturacionRequest.builder().razonSocial(razon).nit("901000000-0").direccion("Calle 10 # 20-30")
                .ciudad("Medellín").correo("facturas@biolearn.co").telefono("604 444 5566").build();
    }

    private static DatosRecaudoRequest recaudo(String numero) {
        return DatosRecaudoRequest.builder().banco("Davivienda").tipoCuenta("corriente").numeroCuenta(numero)
                .titular("BioLearn S.A.S.").documentoTitular("901000000-0").telefonoNequi("3001112233").instrucciones("Escribe tu número de pago").build();
    }

    @BeforeEach
    void datos() {
        admin = usuario(1, RolUsuario.admin);
        financiero = usuario(40, RolUsuario.contador); contador(financiero, RolContador.administrador_financiero);
        auxiliar = usuario(41, RolUsuario.contador); contador(auxiliar, RolContador.auxiliar);
        auditor = usuario(42, RolUsuario.contador); contador(auditor, RolContador.auditor);
        estudianteU = usuario(30, RolUsuario.estudiante);
        vigente = DatosFacturacion.builder().id(1).razonSocial("BioLearn S.A.S.").nit("901000000-0").direccion("Calle 10 # 20-30")
                .ciudad("Medellín").correo("facturas@biolearn.co").telefono("604 444 5566").logoUrl("/logo.png")
                .actualizadoEn(LocalDateTime.now().minusDays(3)).build();
        cuenta = DatosRecaudo.builder().id(1).banco("Bancolombia").tipoCuenta("ahorros").numeroCuenta("00012345678")
                .titular("BioLearn S.A.S.").documentoTitular("901000000-0").actualizadoPor(admin).actualizadoEn(LocalDateTime.now().minusDays(3)).build();
        when(datosFacturacionRepository.findFirstByOrderByActualizadoEnDescIdDesc()).thenReturn(Optional.of(vigente));
        when(datosRecaudoRepository.findFirstByOrderByActualizadoEnDescIdDesc()).thenReturn(Optional.of(cuenta));
        when(datosFacturacionRepository.save(any(DatosFacturacion.class))).thenAnswer(a -> { DatosFacturacion d = a.getArgument(0); d.setId(2); return d; });
        when(datosRecaudoRepository.save(any(DatosRecaudo.class))).thenAnswer(a -> { DatosRecaudo d = a.getArgument(0); d.setId(2); return d; });
    }

    @Nested @DisplayName("Permisos y configuración")
    class Configuracion {

        @Test @DisplayName("Editan el Administrador y el administrador financiero; el auxiliar y el auditor solo consultan")
        void puedeEditar() {
            assertThat(servicio.configuracion(admin).isPuedeEditar()).isTrue();
            assertThat(servicio.configuracion(financiero).isPuedeEditar()).isTrue();
            assertThat(servicio.configuracion(auxiliar).isPuedeEditar()).isFalse();
            ConfiguracionFinancieraResponse r = servicio.configuracion(auditor);
            assertThat(r.isPuedeEditar()).isFalse();
            assertThat(r.getFacturacion().getRazonSocial()).isEqualTo("BioLearn S.A.S.");
            assertThat(r.getRecaudo().getNumeroCuenta()).isEqualTo("00012345678");
        }

        @Test @DisplayName("Un contador inactivo no edita")
        void contadorInactivo() {
            when(perfilContadorRepository.findByUsuario_Id(40)).thenReturn(Optional.of(PerfilContador.builder().id(40).usuario(financiero)
                    .rolContador(RolContador.administrador_financiero).estado(EstadoPerfil.inactivo).build()));
            assertThat(servicio.configuracion(financiero).isPuedeEditar()).isFalse();
        }
    }

    @Nested @DisplayName("Datos de facturación")
    class Facturacion {

        @Test @DisplayName("Guardar crea una versión nueva con su autor y conserva el logo; la anterior no se toca")
        void nuevaVersion() {
            DatosFacturacionResponse r = servicio.actualizarFacturacion(facturacion("BioLearn Colombia S.A.S."), financiero);
            assertThat(r.getId()).isEqualTo(2);
            assertThat(r.getActualizadoPor()).isEqualTo("N40 A");
            assertThat(r.isVigente()).isTrue();
            verify(datosFacturacionRepository).save(argThat(d -> d.getId() == 2 && "/logo.png".equals(d.getLogoUrl()) && d.getActualizadoPor() == financiero));
            assertThat(vigente.getRazonSocial()).isEqualTo("BioLearn S.A.S.");
        }

        @Test @DisplayName("Sin cambios → 409 «No hay cambios para guardar»")
        void sinCambios() {
            assertThatThrownBy(() -> servicio.actualizarFacturacion(facturacion("BioLearn S.A.S."), admin))
                    .isInstanceOf(SolicitudInvalidaException.class).hasMessage("No hay cambios para guardar");
            verify(datosFacturacionRepository, never()).save(any());
        }

        @Test @DisplayName("El auxiliar, el auditor y otros roles → 403")
        void sinPermiso() {
            for (Usuario u : List.of(auxiliar, auditor, estudianteU))
                assertThatThrownBy(() -> servicio.actualizarFacturacion(facturacion("Otra"), u))
                        .isInstanceOf(SinPermisoException.class).hasMessage(FinanzasServiceImpl.SIN_PERMISO);
            verify(datosFacturacionRepository, never()).save(any());
        }

        @Test @DisplayName("El historial marca solo la primera versión como vigente")
        void historial() {
            DatosFacturacion vieja = DatosFacturacion.builder().id(0).razonSocial("BioLearn").nit("1").actualizadoEn(LocalDateTime.now().minusYears(1)).build();
            when(datosFacturacionRepository.findAllByOrderByActualizadoEnDescIdDesc()).thenReturn(List.of(vigente, vieja));
            List<DatosFacturacionResponse> h = servicio.historialFacturacion();
            assertThat(h).extracting(DatosFacturacionResponse::isVigente).containsExactly(true, false);
            assertThat(h.get(1).getActualizadoPor()).isNull();
        }

        @Test @DisplayName("La dirección del emisor incluye la ciudad cuando no la trae")
        void direccionCompleta() {
            assertThat(FinanzasServiceImpl.direccionCompleta(vigente)).isEqualTo("Calle 10 # 20-30, Medellín");
            assertThat(FinanzasServiceImpl.direccionCompleta(DatosFacturacion.builder().direccion("Medellín, Antioquia").ciudad("Medellín").build()))
                    .isEqualTo("Medellín, Antioquia");
            assertThat(FinanzasServiceImpl.direccionCompleta(DatosFacturacion.builder().ciudad("Bogotá").build())).isEqualTo("Bogotá");
            assertThat(FinanzasServiceImpl.direccionCompleta(null)).isNull();
        }
    }

    @Nested @DisplayName("Datos de recaudo")
    class Recaudo {

        @Test @DisplayName("El administrador financiero guarda una cuenta nueva; el estudiante ve la vigente sin el autor")
        void nuevaCuenta() {
            DatosRecaudoResponse r = servicio.actualizarRecaudo(recaudo("4567891234"), financiero);
            assertThat(r.getBanco()).isEqualTo("Davivienda");
            assertThat(r.getTelefonoNequi()).isEqualTo("3001112233");
            assertThat(servicio.recaudoVigente().getActualizadoPor()).isNull();
        }

        @Test @DisplayName("Sin cambios → 409; el auditor → 403")
        void validaciones() {
            DatosRecaudoRequest igual = DatosRecaudoRequest.builder().banco("Bancolombia").tipoCuenta("ahorros").numeroCuenta("00012345678")
                    .titular("BioLearn S.A.S.").documentoTitular("901000000-0").build();
            assertThatThrownBy(() -> servicio.actualizarRecaudo(igual, admin)).isInstanceOf(SolicitudInvalidaException.class);
            assertThatThrownBy(() -> servicio.actualizarRecaudo(recaudo("4567891234"), auditor)).isInstanceOf(SinPermisoException.class);
        }

        @Test @DisplayName("Sin cuenta registrada, el estudiante recibe 404 con una alternativa")
        void sinCuenta() {
            when(datosRecaudoRepository.findFirstByOrderByActualizadoEnDescIdDesc()).thenReturn(Optional.empty());
            assertThatThrownBy(() -> servicio.recaudoVigente()).isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("paga en línea");
        }
    }

    @Nested @DisplayName("Inicio del contador")
    class Resumen {

        private Pago pago(int id, String medio, String estado, LocalDateTime fecha, String monto) {
            Usuario u = usuario(100 + id, RolUsuario.estudiante);
            return Pago.builder().id(id).medioPago(medio).estado(estado).fechaPago(fecha).creadoEn(fecha).monto(new BigDecimal(monto))
                    .estudiante(PerfilEstudiante.builder().id(id).usuario(u).build()).build();
        }

        @Test @DisplayName("Cifras del mes, pendientes, seis meses y movimientos del más nuevo al más antiguo")
        void resumen() {
            LocalDateTime ahora = LocalDateTime.now().withHour(12);
            Pago p1 = pago(1, "wompi_tarjeta", "aprobado", ahora.minusMinutes(30), "100000");
            Pago p2 = pago(2, "transferencia", "reembolsado", ahora.minusMinutes(20), "50000");
            Pago viejo = pago(3, "pse", "aprobado", ahora.minusMonths(2), "70000");
            when(pagoRepository.findByEstadoInAndFechaPagoBetweenOrderByFechaPagoAsc(anyCollection(), any(), any())).thenReturn(List.of(viejo, p1, p2));
            Reembolso devuelto = Reembolso.builder().id(7).pago(p2).solicitante(p2.getEstudiante().getUsuario()).estado("devuelto")
                    .monto(new BigDecimal("50000")).solicitadoEn(ahora.minusMinutes(15)).devueltoEn(ahora.minusMinutes(10)).build();
            when(reembolsoRepository.findByEstadoAndDevueltoEnBetween(eq("devuelto"), any(), any())).thenReturn(List.of(devuelto));
            when(reembolsoRepository.findBySolicitadoEnBetweenOrderBySolicitadoEnAsc(any(), any())).thenReturn(List.of(devuelto));
            Pago conComprobante = pago(4, "transferencia", "pendiente", ahora, "85000"); conComprobante.setComprobanteUrl("/api/archivos/x.pdf");
            Pago sinComprobante = pago(5, "transferencia", "pendiente", ahora, "85000");
            Pago tarjetaPendiente = pago(6, "wompi_tarjeta", "pendiente", ahora, "85000");
            when(pagoRepository.findByEstadoOrderByCreadoEnDesc("pendiente")).thenReturn(List.of(conComprobante, sinComprobante, tarjetaPendiente));
            when(reembolsoRepository.findByEstado("solicitado")).thenReturn(List.of(new Reembolso(), new Reembolso()));
            when(reembolsoRepository.findByEstado("aprobado")).thenReturn(List.of(Reembolso.builder().monto(new BigDecimal("65000")).build()));

            ResumenFinancieroResponse r = servicio.resumen();
            assertThat(r.getIngresosMes()).isEqualByComparingTo("150000");
            assertThat(r.getPagosMes()).isEqualTo(2);
            assertThat(r.getReembolsadoMes()).isEqualByComparingTo("50000");
            assertThat(r.getNetoMes()).isEqualByComparingTo("100000");
            assertThat(r.getTransferenciasPorConfirmar()).isEqualTo(1);
            assertThat(r.getReembolsosPorRevisar()).isEqualTo(2);
            assertThat(r.getReembolsosPorDevolver()).isEqualTo(1);
            assertThat(r.getMontoPorDevolver()).isEqualByComparingTo("65000");
            assertThat(r.getUltimosMeses()).hasSize(6);
            assertThat(r.getUltimosMeses().get(3).getIngresos()).isEqualByComparingTo("70000");   // hace dos meses
            assertThat(r.getMovimientos()).extracting(ResumenFinancieroResponse.Movimiento::getTipo).containsExactly("reembolso", "pago", "pago", "pago");
            assertThat(r.getMovimientos().get(0).getEstudiante()).isEqualTo("N102 A");
        }
    }
}
