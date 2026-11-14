package co.biolearn.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enlaces firmados con vencimiento para los documentos PDF de los cursos (recomendación de la Fase 2).
 * La API entrega /api/archivos/<uuid>.pdf?exp=<segundos>&firma=<HMAC-SHA256>; un enlace sin firma, alterado
 * o vencido responde 403. Las imágenes (portadas y editor) siguen públicas porque no son contenido protegido.
 */
@Component
public class ArchivoFirma {

    static final long VIGENCIA_SEGUNDOS = 2 * 60 * 60;
    private static final Pattern DOCUMENTO = Pattern.compile("^/api/archivos/([0-9a-f-]{36}\\.pdf)(\\?.*)?$");

    private final byte[] secreto;
    private final Clock reloj;

    /** Constructor que usa Spring (hay otro, con reloj fijo, solo para las pruebas unitarias). */
    @org.springframework.beans.factory.annotation.Autowired
    public ArchivoFirma(@Value("${biolearn.archivos.secreto:${biolearn.jwt.secret}}") String secreto) {
        this(secreto, Clock.systemUTC());
    }

    ArchivoFirma(String secreto, Clock reloj) {
        this.secreto = ("archivos:" + secreto).getBytes(StandardCharsets.UTF_8);
        this.reloj = reloj;
    }

    /** Firma la URL si es un documento PDF de la plataforma; cualquier otra URL se devuelve igual. */
    public String firmar(String url) {
        if (url == null) return null;
        Matcher m = DOCUMENTO.matcher(url);
        if (!m.matches()) return url;
        long exp = reloj.instant().getEpochSecond() + VIGENCIA_SEGUNDOS;
        return "/api/archivos/" + m.group(1) + "?exp=" + exp + "&firma=" + hmac(m.group(1), exp);
    }

    /** true si el nombre requiere firma (PDF). */
    public boolean requiereFirma(String nombre) { return nombre != null && nombre.endsWith(".pdf"); }

    public boolean valida(String nombre, Long exp, String firma) {
        if (exp == null || firma == null || exp < reloj.instant().getEpochSecond()) return false;
        return MessageDigest.isEqual(hmac(nombre, exp).getBytes(StandardCharsets.UTF_8), firma.getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String nombre, long exp) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((nombre + ":" + exp).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el enlace", e);
        }
    }
}
