package co.biolearn.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XlsxWriter — libros de Excel sin dependencias (Pagos, Fase 3)")
class XlsxWriterTest {

    private static Map<String, String> abrir(byte[] xlsx) throws Exception {
        Map<String, String> partes = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) partes.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
        }
        return partes;
    }

    @Test @DisplayName("El libro trae las partes de Office Open XML, una hoja por cada hoja agregada y los estilos")
    void partes() throws Exception {
        XlsxWriter x = new XlsxWriter();
        x.hoja("Resumen", 30, 15).titulo("Reporte").par("Ingresos", new BigDecimal("1500.50"), XlsxWriter.Tipo.MONEDA);
        x.hoja("Detalle").encabezado(List.of("Fecha", "Monto"))
                .fila(List.of(LocalDateTime.of(2026, 11, 16, 12, 0), new BigDecimal("85000")), List.of(XlsxWriter.Tipo.FECHA, XlsxWriter.Tipo.MONEDA));
        Map<String, String> p = abrir(x.bytes());
        assertThat(p).containsKeys("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
                "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml");
        assertThat(p.get("xl/workbook.xml")).contains("name=\"Resumen\"").contains("name=\"Detalle\"");
        assertThat(p.get("xl/worksheets/sheet1.xml")).contains("<v>1500.5</v>").contains("<mergeCell ref=\"A1:B1\"/>");
        assertThat(p.get("xl/worksheets/sheet2.xml")).contains("state=\"frozen\"").contains("<v>85000</v>");
    }

    @Test @DisplayName("Nombres de hoja sin caracteres prohibidos, de máximo 31 caracteres y sin repetirse")
    void nombresDeHoja() {
        XlsxWriter x = new XlsxWriter();
        assertThat(x.hoja("Pagos: aprobados / [nov]*").getNombre()).isEqualTo("Pagos aprobados nov");
        assertThat(x.hoja("Una hoja con un nombre demasiado largo para Excel").getNombre()).hasSizeLessThanOrEqualTo(31);
        x.hoja("Detalle");
        assertThat(x.hoja("detalle").getNombre()).isEqualTo("detalle (2)");
    }

    @Test @DisplayName("Escapa el XML y quita caracteres de control")
    void escapa() {
        assertThat(XlsxWriter.xml("A & B <c> \"d\"\u0001")).isEqualTo("A &amp; B &lt;c&gt; &quot;d&quot;");
    }

    @Test @DisplayName("Fechas como número de serie de Excel y referencias A1")
    void fechasYReferencias() {
        assertThat(XlsxWriter.serial(LocalDate.of(2026, 1, 1))).isEqualTo("46023");
        assertThat(XlsxWriter.serial(LocalDateTime.of(2026, 1, 1, 12, 0))).isEqualTo("46023.5");
        assertThat(XlsxWriter.ref(0, 1)).isEqualTo("A1");
        assertThat(XlsxWriter.ref(26, 3)).isEqualTo("AA3");
    }
}
