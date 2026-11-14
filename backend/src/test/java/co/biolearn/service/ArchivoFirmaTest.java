package co.biolearn.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArchivoFirma — enlaces firmados de los documentos (Fase 3)")
class ArchivoFirmaTest {

    private static final String NOMBRE = "123e4567-e89b-12d3-a456-426614174000.pdf";
    private final Clock ahora = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    private static long exp(String url) { Matcher m = Pattern.compile("exp=(\\d+)").matcher(url); m.find(); return Long.parseLong(m.group(1)); }
    private static String firma(String url) { Matcher m = Pattern.compile("firma=([0-9a-f]+)").matcher(url); m.find(); return m.group(1); }

    @Test @DisplayName("Firma los PDF con vencimiento de 2 horas y deja igual las demás URL")
    void firmar() {
        ArchivoFirma f = new ArchivoFirma("secreto", ahora);
        String url = f.firmar("/api/archivos/" + NOMBRE);
        assertThat(url).startsWith("/api/archivos/" + NOMBRE + "?exp=").contains("&firma=");
        assertThat(exp(url)).isEqualTo(ahora.instant().getEpochSecond() + ArchivoFirma.VIGENCIA_SEGUNDOS);
        assertThat(f.firmar("/api/archivos/123e4567-e89b-12d3-a456-426614174000.png")).doesNotContain("firma");
        assertThat(f.firmar("https://www.youtube.com/embed/x")).isEqualTo("https://www.youtube.com/embed/x");
        assertThat(f.firmar(null)).isNull();
    }

    @Test @DisplayName("Una firma válida se acepta; alterada o de otro archivo se rechaza")
    void validar() {
        ArchivoFirma f = new ArchivoFirma("secreto", ahora);
        String url = f.firmar("/api/archivos/" + NOMBRE);
        assertThat(f.valida(NOMBRE, exp(url), firma(url))).isTrue();
        assertThat(f.valida(NOMBRE, exp(url), firma(url).replace('a', 'b').replace('1', '2'))).isFalse();
        assertThat(f.valida("otro0000-e89b-12d3-a456-426614174000.pdf", exp(url), firma(url))).isFalse();
        assertThat(f.valida(NOMBRE, null, null)).isFalse();
    }

    @Test @DisplayName("Un enlace vencido se rechaza")
    void vencido() {
        ArchivoFirma antes = new ArchivoFirma("secreto", ahora);
        String url = antes.firmar("/api/archivos/" + NOMBRE);
        ArchivoFirma despues = new ArchivoFirma("secreto", Clock.offset(ahora, java.time.Duration.ofHours(3)));
        assertThat(despues.valida(NOMBRE, exp(url), firma(url))).isFalse();
    }

    @Test @DisplayName("Volver a firmar una URL ya firmada reemplaza la firma anterior")
    void refirmar() {
        ArchivoFirma f = new ArchivoFirma("secreto", ahora);
        String url = f.firmar(f.firmar("/api/archivos/" + NOMBRE));
        assertThat(url.split("firma=")).hasSize(2);
    }

    @Test @DisplayName("Solo los PDF requieren firma")
    void requiere() {
        ArchivoFirma f = new ArchivoFirma("secreto", ahora);
        assertThat(f.requiereFirma(NOMBRE)).isTrue();
        assertThat(f.requiereFirma("a.png")).isFalse();
    }
}
