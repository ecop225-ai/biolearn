package co.biolearn.service;

import co.biolearn.dto.PagoPasarelaRequest;
import co.biolearn.model.Pago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PasarelaPruebas — datos de prueba del sandbox de Wompi (Pagos, Fase 1)")
class PasarelaPruebasTest {

    private final PasarelaPruebas pasarela = new PasarelaPruebas();

    private static Pago pago(String medio) { return Pago.builder().id(1).medioPago(medio).build(); }

    @Test @DisplayName("Tarjeta 4242 4242 4242 4242 aprueba (con espacios) y 4111 1111 1111 1111 rechaza")
    void tarjeta() {
        PasarelaPago.Resultado ok = pasarela.cobrar(pago("wompi_tarjeta"), PagoPasarelaRequest.builder().numeroTarjeta("4242 4242 4242 4242").build());
        assertThat(ok.aprobado()).isTrue();
        assertThat(ok.referencia()).startsWith("PRUEBA-").hasSize(15);
        PasarelaPago.Resultado no = pasarela.cobrar(pago("wompi_tarjeta"), PagoPasarelaRequest.builder().numeroTarjeta("4111111111111111").build());
        assertThat(no.aprobado()).isFalse();
        assertThat(no.motivo()).isEqualTo("La entidad financiera rechazó la tarjeta");
    }

    @Test @DisplayName("Nequi 3991111111 aprueba y 3992222222 rechaza")
    void nequi() {
        assertThat(pasarela.cobrar(pago("wompi_nequi"), PagoPasarelaRequest.builder().telefono("3991111111").build()).aprobado()).isTrue();
        assertThat(pasarela.cobrar(pago("wompi_nequi"), PagoPasarelaRequest.builder().telefono("3992222222").build()).aprobado()).isFalse();
    }

    @Test @DisplayName("PSE: banco 1 aprueba y banco 2 rechaza")
    void pse() {
        assertThat(pasarela.cobrar(pago("pse"), PagoPasarelaRequest.builder().banco("1").build()).aprobado()).isTrue();
        assertThat(pasarela.cobrar(pago("pse"), PagoPasarelaRequest.builder().banco("2").build()).motivo()).isEqualTo("El banco no autorizó el débito");
    }

    @Test @DisplayName("Un dato que no es de prueba, o sin datos → 400 con los datos válidos")
    void invalidos() {
        assertThatThrownBy(() -> pasarela.cobrar(pago("wompi_tarjeta"), PagoPasarelaRequest.builder().numeroTarjeta("5555").build()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("4242 4242 4242 4242");
        assertThatThrownBy(() -> pasarela.cobrar(pago("pse"), null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("banco de prueba");
        assertThatThrownBy(() -> pasarela.cobrar(pago("transferencia"), new PagoPasarelaRequest())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("Reembolso (Fase 2): tarjeta y Nequi se devuelven al mismo medio; PSE, transferencia o un valor mayor al pago no")
    void reembolsar() {
        Pago tarjeta = pago("wompi_tarjeta"); tarjeta.setMonto(new java.math.BigDecimal("95000"));
        assertThat(pasarela.reembolsar(tarjeta, new java.math.BigDecimal("50000"))).startsWith("PRUEBA-REV-");
        Pago nequi = pago("wompi_nequi"); nequi.setMonto(new java.math.BigDecimal("95000"));
        assertThat(pasarela.reembolsar(nequi, new java.math.BigDecimal("95000"))).startsWith("PRUEBA-REV-");
        assertThatThrownBy(() -> pasarela.reembolsar(tarjeta, new java.math.BigDecimal("95001"))).isInstanceOf(IllegalArgumentException.class);
        Pago pse = pago("pse"); pse.setMonto(new java.math.BigDecimal("95000"));
        assertThatThrownBy(() -> pasarela.reembolsar(pse, new java.math.BigDecimal("1000"))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tarjeta o Nequi");
    }
}
