package co.biolearn.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Política de contraseñas (RNF-022)")
class PoliticaContrasenaTest {

    @ParameterizedTest(name = "\"{0}\" es válida")
    @ValueSource(strings = {"Clave1234", "Docente2026", "ABCDEFG1", "Mi.Clave#99"})
    void contrasenasValidas(String contrasena) {
        assertThat(contrasena.matches(PoliticaContrasena.PATRON)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" es rechazada")
    @ValueSource(strings = {"clave1234", "CLAVESINNUMERO", "Corta1", "12345678", ""})
    void contrasenasInvalidas(String contrasena) {
        assertThat(contrasena.matches(PoliticaContrasena.PATRON)).isFalse();
    }
}
