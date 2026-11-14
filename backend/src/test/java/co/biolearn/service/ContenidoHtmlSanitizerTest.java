package co.biolearn.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContenidoHtmlSanitizer — limpieza del HTML del editor (Fase 3)")
class ContenidoHtmlSanitizerTest {

    @Test @DisplayName("Elimina scripts, eventos y enlaces javascript:")
    void eliminaCodigo() {
        String r = ContenidoHtmlSanitizer.limpiar("<p onclick=\"x()\">Hola</p><script>alert(1)</script><a href=\"javascript:robar()\">clic</a>");
        assertThat(r).contains("Hola").doesNotContain("script").doesNotContain("onclick").doesNotContain("javascript:");
    }

    @Test @DisplayName("Conserva el formato del editor: estilos, negrita, listas, tablas e imágenes")
    void conservaFormato() {
        String html = "<div style=\"background:#E5EDE6;border-left:4px solid #5C7A63\"><strong>Dato</strong></div><ul><li>uno</li></ul>"
                + "<table><tr><td style=\"padding:14px\">celda</td></tr></table><img src=\"http://localhost:8080/api/archivos/a.png\">";
        String r = ContenidoHtmlSanitizer.limpiar(html);
        assertThat(r).contains("background:#E5EDE6").contains("<strong>Dato</strong>").contains("<li>uno</li>").contains("<td").contains("<img");
    }

    @Test @DisplayName("Conserva los videos de YouTube y elimina iframes de otros sitios")
    void iframes() {
        String r = ContenidoHtmlSanitizer.limpiar("<iframe src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\"></iframe><iframe src=\"https://sitio-malicioso.com/x\"></iframe>");
        assertThat(r).contains("youtube.com/embed/dQw4w9WgXcQ").doesNotContain("sitio-malicioso");
    }

    @Test @DisplayName("Quita estilos peligrosos (expression, url)")
    void estilosPeligrosos() {
        String r = ContenidoHtmlSanitizer.limpiar("<p style=\"background:url(javascript:alert(1))\">x</p>");
        assertThat(r).doesNotContain("url(").contains("x");
    }

    @Test @DisplayName("Texto vacío o nulo se devuelve igual")
    void vacio() {
        assertThat(ContenidoHtmlSanitizer.limpiar(null)).isNull();
        assertThat(ContenidoHtmlSanitizer.limpiar("  ")).isEqualTo("  ");
    }
}
