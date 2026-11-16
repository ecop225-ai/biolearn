package co.biolearn.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Escritor mínimo de libros de Excel (.xlsx, Office Open XML) sin dependencias externas (Pagos, Fase 3).
 * Admite varias hojas, títulos, encabezados con relleno, celdas de texto, número, moneda, porcentaje y fecha,
 * filas de total, anchos de columna y el encabezado de la tabla inmovilizado.
 */
public class XlsxWriter {

    /** Formato de una celda. */
    public enum Tipo { TEXTO, NUMERO, MONEDA, PORCENTAJE, FECHA }

    // Índices de cellXfs en styles.xml
    private static final int S_NORMAL = 0, S_TITULO = 1, S_ENCABEZADO = 2, S_TEXTO = 3, S_MONEDA = 4, S_NUMERO = 5,
            S_FECHA = 6, S_TOTAL_TEXTO = 7, S_TOTAL_MONEDA = 8, S_TOTAL_NUMERO = 9, S_NOTA = 10, S_PORCENTAJE = 11,
            S_SUBTITULO = 12, S_TOTAL_PORCENTAJE = 13;

    private final List<Hoja> hojas = new ArrayList<>();
    private final Set<String> nombres = new HashSet<>();

    /** Agrega una hoja. El nombre se limpia (sin []:*?/\) y se acorta a 31 caracteres; si se repite, se numera. */
    public Hoja hoja(String nombre, double... anchos) {
        String base = (nombre == null || nombre.isBlank() ? "Hoja" : nombre).replaceAll("[\\[\\]:*?/\\\\]", " ").replaceAll("\\s+", " ").trim();
        if (base.length() > 31) base = base.substring(0, 31).trim();
        String n = base;
        for (int i = 2; nombres.contains(n.toLowerCase()); i++) {
            String suf = " (" + i + ")";
            n = (base.length() + suf.length() > 31 ? base.substring(0, 31 - suf.length()) : base) + suf;
        }
        nombres.add(n.toLowerCase());
        Hoja h = new Hoja(n, anchos);
        hojas.add(h);
        return h;
    }

    public List<Hoja> getHojas() { return hojas; }

    /** Una hoja del libro: se escribe fila por fila. */
    public static class Hoja {
        private final String nombre;
        private final double[] anchos;
        private final StringBuilder filas = new StringBuilder();
        private final List<String> combinadas = new ArrayList<>();
        private int fila = 0;
        private int filaInmovilizada = 0;

        Hoja(String nombre, double[] anchos) { this.nombre = nombre; this.anchos = anchos == null ? new double[0] : anchos; }

        public String getNombre() { return nombre; }

        /** Número de filas escritas. */
        public int getFilas() { return fila; }

        public Hoja titulo(String texto) { return celdaUnica(texto, S_TITULO, 22); }

        public Hoja subtitulo(String texto) { return celdaUnica(texto, S_SUBTITULO, 18); }

        public Hoja nota(String texto) { return celdaUnica(texto, S_NOTA, 0); }

        public Hoja vacia() { fila++; return this; }

        /** Etiqueta en negrita y valor con su formato (tarjetas de indicadores). */
        public Hoja par(String etiqueta, Object valor, Tipo tipo) {
            fila++;
            filas.append("<row r=\"").append(fila).append("\">");
            celda(0, etiqueta, S_TOTAL_TEXTO);
            celda(1, valor, estilo(tipo, false));
            filas.append("</row>");
            return this;
        }

        /** Encabezado de una tabla; la primera tabla de la hoja queda inmovilizada al desplazarse. */
        public Hoja encabezado(List<String> columnas) {
            fila++;
            filas.append("<row r=\"").append(fila).append("\" ht=\"20\" customHeight=\"1\">");
            for (int i = 0; i < columnas.size(); i++) celda(i, columnas.get(i), S_ENCABEZADO);
            filas.append("</row>");
            if (filaInmovilizada == 0) filaInmovilizada = fila;
            return this;
        }

        public Hoja fila(List<?> valores, List<Tipo> tipos) { return escribirFila(valores, tipos, false); }

        public Hoja total(List<?> valores, List<Tipo> tipos) { return escribirFila(valores, tipos, true); }

        private Hoja escribirFila(List<?> valores, List<Tipo> tipos, boolean total) {
            fila++;
            filas.append("<row r=\"").append(fila).append("\">");
            for (int i = 0; i < valores.size(); i++) {
                Tipo t = tipos != null && i < tipos.size() ? tipos.get(i) : Tipo.TEXTO;
                celda(i, valores.get(i), estilo(t, total));
            }
            filas.append("</row>");
            return this;
        }

        private Hoja celdaUnica(String texto, int estilo, int alto) {
            fila++;
            filas.append("<row r=\"").append(fila).append('"');
            if (alto > 0) filas.append(" ht=\"").append(alto).append("\" customHeight=\"1\"");
            filas.append('>');
            celda(0, texto, estilo);
            filas.append("</row>");
            int ultima = Math.max(anchos.length, 1) - 1;
            if (ultima > 0) combinadas.add(ref(0, fila) + ":" + ref(ultima, fila));
            return this;
        }

        private static int estilo(Tipo t, boolean total) {
            return switch (t) {
                case MONEDA -> total ? S_TOTAL_MONEDA : S_MONEDA;
                case NUMERO -> total ? S_TOTAL_NUMERO : S_NUMERO;
                case PORCENTAJE -> total ? S_TOTAL_PORCENTAJE : S_PORCENTAJE;
                case FECHA -> total ? S_TOTAL_TEXTO : S_FECHA;
                default -> total ? S_TOTAL_TEXTO : S_TEXTO;
            };
        }

        private void celda(int col, Object v, int estilo) {
            String r = ref(col, fila);
            if (v == null) {
                filas.append("<c r=\"").append(r).append("\" s=\"").append(estilo).append("\"/>");
            } else if (v instanceof Number n) {
                filas.append("<c r=\"").append(r).append("\" s=\"").append(estilo).append("\"><v>")
                        .append(numero(n)).append("</v></c>");
            } else if (v instanceof LocalDateTime || v instanceof LocalDate) {
                filas.append("<c r=\"").append(r).append("\" s=\"").append(estilo == S_TOTAL_TEXTO ? S_TOTAL_TEXTO : S_FECHA)
                        .append("\"><v>").append(serial(v)).append("</v></c>");
            } else if (v instanceof Boolean b) {
                texto(r, b ? "Sí" : "No", estilo);
            } else {
                texto(r, v.toString(), estilo);
            }
        }

        private void texto(String r, String s, int estilo) {
            filas.append("<c r=\"").append(r).append("\" s=\"").append(estilo).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(XlsxWriter.xml(s)).append("</t></is></c>");
        }

        String contenido() {
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                    .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                    .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
            sb.append("<sheetViews><sheetView workbookViewId=\"0\"");
            sb.append(filaInmovilizada > 0 ? ">" : "/>");
            if (filaInmovilizada > 0) {
                sb.append("<pane ySplit=\"").append(filaInmovilizada).append("\" topLeftCell=\"A").append(filaInmovilizada + 1)
                        .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView>");
            }
            sb.append("</sheetViews><sheetFormatPr defaultRowHeight=\"15\"/>");
            if (anchos.length > 0) {
                sb.append("<cols>");
                for (int i = 0; i < anchos.length; i++)
                    sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1).append("\" width=\"")
                            .append(anchos[i]).append("\" customWidth=\"1\"/>");
                sb.append("</cols>");
            }
            sb.append("<sheetData>").append(filas).append("</sheetData>");
            if (!combinadas.isEmpty()) {
                sb.append("<mergeCells count=\"").append(combinadas.size()).append("\">");
                combinadas.forEach(c -> sb.append("<mergeCell ref=\"").append(c).append("\"/>"));
                sb.append("</mergeCells>");
            }
            sb.append("<pageMargins left=\"0.5\" right=\"0.5\" top=\"0.6\" bottom=\"0.6\" header=\"0.3\" footer=\"0.3\"/>")
              .append("<pageSetup orientation=\"landscape\" fitToHeight=\"0\"/></worksheet>");
            return sb.toString();
        }
    }

    /** Genera el archivo .xlsx. */
    public byte[] bytes() {
        if (hojas.isEmpty()) hoja("Hoja");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            StringBuilder tipos = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                    .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                    .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                    .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                    .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                    .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
            StringBuilder libro = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                    .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
                    .append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rels = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
                    .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            for (int i = 0; i < hojas.size(); i++) {
                int n = i + 1;
                tipos.append("<Override PartName=\"/xl/worksheets/sheet").append(n)
                        .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                libro.append("<sheet name=\"").append(xml(hojas.get(i).nombre)).append("\" sheetId=\"").append(n)
                        .append("\" r:id=\"rId").append(n).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(n)
                        .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                        .append(n).append(".xml\"/>");
                poner(zip, "xl/worksheets/sheet" + n + ".xml", hojas.get(i).contenido());
            }
            rels.append("<Relationship Id=\"rId").append(hojas.size() + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>");
            poner(zip, "[Content_Types].xml", tipos.append("</Types>").toString());
            poner(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            poner(zip, "xl/workbook.xml", libro.append("</sheets></workbook>").toString());
            poner(zip, "xl/_rels/workbook.xml.rels", rels.toString());
            poner(zip, "xl/styles.xml", ESTILOS);
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el archivo de Excel", e);
        }
    }

    private static void poner(ZipOutputStream zip, String nombre, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(nombre));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Referencia A1 de una celda (columna desde 0). */
    static String ref(int col, int fila) {
        StringBuilder s = new StringBuilder();
        int c = col + 1;
        while (c > 0) { int m = (c - 1) % 26; s.insert(0, (char) ('A' + m)); c = (c - 1) / 26; }
        return s.append(fila).toString();
    }

    private static String numero(Number n) {
        if (n instanceof BigDecimal b) return b.stripTrailingZeros().toPlainString();
        if (n instanceof Double || n instanceof Float) return BigDecimal.valueOf(n.doubleValue()).stripTrailingZeros().toPlainString();
        return n.toString();
    }

    /** Fecha en número de serie de Excel (días desde el 30/12/1899, con la fracción de la hora). */
    static String serial(Object v) {
        LocalDateTime t = v instanceof LocalDate d ? d.atStartOfDay() : (LocalDateTime) v;
        LocalDateTime base = LocalDateTime.of(1899, 12, 30, 0, 0);
        BigDecimal dias = BigDecimal.valueOf(ChronoUnit.SECONDS.between(base, t)).divide(BigDecimal.valueOf(86400), 10, java.math.RoundingMode.HALF_UP);
        return dias.setScale(8, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /** Escapa XML y quita los caracteres de control que Excel no admite. */
    static String xml(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                default -> { if (ch >= 0x20 || ch == '\n' || ch == '\t') b.append(ch); }
            }
        }
        return b.toString();
    }

    private static final String ESTILOS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"4\">"
            + "<numFmt numFmtId=\"164\" formatCode=\"&quot;$&quot;\\ #,##0\"/>"
            + "<numFmt numFmtId=\"165\" formatCode=\"#,##0\"/>"
            + "<numFmt numFmtId=\"166\" formatCode=\"dd/mm/yyyy\\ hh:mm\"/>"
            + "<numFmt numFmtId=\"167\" formatCode=\"0.0%\"/>"
            + "</numFmts>"
            + "<fonts count=\"6\">"
            + "<font><sz val=\"10\"/><name val=\"Arial\"/></font>"
            + "<font><b/><sz val=\"14\"/><color rgb=\"FF1F4E3D\"/><name val=\"Arial\"/></font>"
            + "<font><b/><sz val=\"10\"/><color rgb=\"FFFFFFFF\"/><name val=\"Arial\"/></font>"
            + "<font><b/><sz val=\"10\"/><name val=\"Arial\"/></font>"
            + "<font><i/><sz val=\"9\"/><color rgb=\"FF6B6B6B\"/><name val=\"Arial\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FF1F4E3D\"/><name val=\"Arial\"/></font>"
            + "</fonts>"
            + "<fills count=\"4\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF2F5D50\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE3EEEA\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\">"
            + "<border><left/><right/><top/><bottom/><diagonal/></border>"
            + "<border><left style=\"thin\"><color rgb=\"FFBFBFBF\"/></left><right style=\"thin\"><color rgb=\"FFBFBFBF\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFBFBFBF\"/></top><bottom style=\"thin\"><color rgb=\"FFBFBFBF\"/></bottom><diagonal/></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"14\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"                                                                    // 0 normal
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                                     // 1 título
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"center\" wrapText=\"1\"/></xf>" // 2 encabezado
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>"  // 3 texto
            + "<xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\"/></xf>" // 4 moneda
            + "<xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\"/></xf>" // 5 número
            + "<xf numFmtId=\"166\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"left\" vertical=\"top\"/></xf>" // 6 fecha
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment vertical=\"top\" wrapText=\"1\"/></xf>" // 7 total texto
            + "<xf numFmtId=\"164\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"   // 8 total moneda
            + "<xf numFmtId=\"165\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"   // 9 total número
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyAlignment=\"1\"><alignment wrapText=\"1\"/></xf>" // 10 nota
            + "<xf numFmtId=\"167\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\"/>"                      // 11 porcentaje
            + "<xf numFmtId=\"0\" fontId=\"5\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"                                                 // 12 subtítulo
            + "<xf numFmtId=\"167\" fontId=\"3\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>"   // 13 total porcentaje
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
