package co.biolearn.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.List;
import java.util.Locale;

/**
 * Limpia el HTML que escriben los docentes con el editor enriquecido antes de guardarlo y mostrarlo a los
 * estudiantes (recomendación de la Fase 2). Conserva el formato del editor (estilos, tablas, imágenes, bloques)
 * y los videos de proveedores conocidos; elimina scripts, eventos (onclick…), enlaces javascript: y cualquier
 * iframe de otro origen.
 */
public final class ContenidoHtmlSanitizer {

    /** Proveedores cuyos reproductores se pueden incrustar en las lecturas. */
    static final List<String> IFRAMES_PERMITIDOS = List.of(
            "https://www.youtube.com/embed/", "https://www.youtube-nocookie.com/embed/", "https://player.vimeo.com/video/",
            "https://wordwall.net/", "https://view.genial.ly/", "https://phet.colorado.edu/");

    private static final Safelist LISTA = Safelist.relaxed()
            .addTags("span", "div", "hr", "figure", "figcaption", "u", "s", "strike", "font", "iframe", "mark")
            .addAttributes(":all", "style", "class", "align")
            .addAttributes("font", "color", "face", "size")
            .addAttributes("td", "colspan", "rowspan", "width")
            .addAttributes("th", "colspan", "rowspan", "width")
            .addAttributes("iframe", "src", "width", "height", "allow", "allowfullscreen", "frameborder", "title")
            .addProtocols("img", "src", "http", "https", "data")
            .addProtocols("iframe", "src", "https")
            .preserveRelativeLinks(true);

    private ContenidoHtmlSanitizer() { }

    public static String limpiar(String html) {
        if (html == null || html.isBlank()) return html;
        Document.OutputSettings salida = new Document.OutputSettings().prettyPrint(false);
        String limpio = Jsoup.clean(html, "http://localhost", LISTA, salida);
        Document doc = Jsoup.parseBodyFragment(limpio);
        doc.outputSettings(salida);
        for (Element iframe : doc.select("iframe")) {
            String src = iframe.attr("src");
            if (IFRAMES_PERMITIDOS.stream().noneMatch(src::startsWith)) iframe.remove();
        }
        for (Element el : doc.select("[style]")) {
            String estilo = el.attr("style").toLowerCase(Locale.ROOT);
            if (estilo.contains("javascript:") || estilo.contains("expression(") || estilo.contains("url(")) el.removeAttr("style");
        }
        return doc.body().html();
    }
}
