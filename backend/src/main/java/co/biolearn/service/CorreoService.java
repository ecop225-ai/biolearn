package co.biolearn.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Envío de correos de BioLearn (activación, invitación, recuperación de contraseña y facturas) por SMTP,
 * por ejemplo con Gmail y una contraseña de aplicación (ver application.properties.example).
 *
 * - Sin configurar (biolearn.correo.habilitado=false, el valor por defecto) no se envía nada: el contenido
 *   queda en la consola, como antes, y las pruebas no dependen de internet.
 * - biolearn.correo.redirigir-a manda TODOS los correos a ese buzón durante el desarrollo, con el destinatario
 *   original en el asunto, para no escribirle a las cuentas de prueba (@biolearn.co no es un buzón propio).
 * - El envío corre en segundo plano y un error de SMTP nunca interrumpe la operación del usuario: se registra en el log.
 */
@Slf4j
@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean habilitado;
    private final String remitente;
    private final String redirigirA;
    private final String urlAuth;
    private final ExecutorService envios = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "biolearn-correo");
        t.setDaemon(true);
        return t;
    });

    public CorreoService(ObjectProvider<JavaMailSender> mailSender,
                         @Value("${biolearn.correo.habilitado:false}") boolean habilitado,
                         @Value("${biolearn.correo.remitente:${spring.mail.username:}}") String remitente,
                         @Value("${biolearn.correo.redirigir-a:}") String redirigirA,
                         @Value("${biolearn.frontend.url-auth:http://127.0.0.1:5173/prototipo-auth/biolearn_auth_prototipo.html}") String urlAuth) {
        this.mailSender = mailSender;
        this.habilitado = habilitado;
        this.remitente = remitente;
        this.redirigirA = redirigirA == null ? "" : redirigirA.trim();
        this.urlAuth = urlAuth;
    }

    // ── correos de la plataforma ────────────────────────────────────────────

    public void activacionEstudiante(String correo, String nombre, String token, int horas) {
        String enlace = urlAuth + "?verificar=" + token;
        enviar(correo, "Activa tu cuenta de BioLearn",
                plantilla("¡Hola, " + nombre + "!",
                        "<p>Gracias por registrarte en BioLearn. Para activar tu cuenta, abre el siguiente enlace:</p>",
                        "Activar mi cuenta", enlace,
                        "El enlace vence en " + horas + " horas. Si no creaste esta cuenta, ignora este correo."));
    }

    public void invitacionPersonal(String correo, String nombre, String rol, String token, int horas) {
        String enlace = urlAuth + "?invitacion=" + token;
        enviar(correo, "Te invitaron a BioLearn",
                plantilla("¡Hola, " + nombre + "!",
                        "<p>El administrador de BioLearn creó tu cuenta con el rol <strong>" + esc(rol) + "</strong>. "
                                + "Para activarla, abre el enlace y define tu contraseña:</p>",
                        "Activar mi cuenta", enlace,
                        "La invitación vence en " + (horas / 24) + " días."));
    }

    public void recuperacion(String correo, String nombre, String token, int minutos) {
        String enlace = urlAuth + "?recuperar=" + token;
        enviar(correo, "Restablece tu contraseña de BioLearn",
                plantilla("Hola, " + nombre,
                        "<p>Recibimos una solicitud para restablecer tu contraseña. Abre el enlace para crear una nueva:</p>",
                        "Crear una contraseña nueva", enlace,
                        "El enlace vence en " + minutos + " minutos. Si no lo solicitaste, ignora este correo: tu contraseña no cambia."));
    }

    /** Factura: el detalle llega ya armado como filas HTML desde el servicio de pagos. */
    public void factura(String correo, String nombre, String numero, String filasHtml, String total, String pie) {
        String cuerpo = "<p>Tu pago fue aprobado. Este es el detalle de tu factura <strong>" + esc(numero) + "</strong>:</p>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;margin:10px 0\">"
                + "<tr style=\"background:#E4EEE9\"><th style=\"text-align:left;padding:8px\">Concepto</th><th style=\"text-align:right;padding:8px\">Valor</th></tr>"
                + filasHtml
                + "<tr><td style=\"padding:8px;font-weight:800\">Total</td><td style=\"padding:8px;text-align:right;font-weight:800\">" + esc(total) + "</td></tr></table>"
                + "<p style=\"font-size:13px;color:#555\">" + pie + "</p>";
        enviar(correo, "Factura " + numero + " · BioLearn",
                plantilla("¡Hola, " + nombre + "!", cuerpo, null, null,
                        "También puedes consultarla en «Mis pagos». Proyecto académico sin validez fiscal."));
    }

    /** Nota crédito de un reembolso aprobado (Pagos, Fase 2); el detalle llega como filas HTML. */
    public void notaCredito(String correo, String nombre, String numero, String numeroFactura, String filasHtml, String total, String pie) {
        String cuerpo = "<p>Tu solicitud de reembolso fue aprobada. Emitimos la nota crédito <strong>" + esc(numero)
                + "</strong> sobre tu factura <strong>" + esc(numeroFactura) + "</strong>:</p>"
                + "<table style=\"width:100%;border-collapse:collapse;font-size:14px;margin:10px 0\">"
                + "<tr style=\"background:#E4EEE9\"><th style=\"text-align:left;padding:8px\">Curso reembolsado</th><th style=\"text-align:right;padding:8px\">Valor</th></tr>"
                + filasHtml
                + "<tr><td style=\"padding:8px;font-weight:800\">Total a devolver</td><td style=\"padding:8px;text-align:right;font-weight:800\">" + esc(total) + "</td></tr></table>"
                + "<p style=\"font-size:13px;color:#555\">" + pie + "</p>";
        enviar(correo, "Nota crédito " + numero + " · BioLearn",
                plantilla("¡Hola, " + nombre + "!", cuerpo, null, null,
                        "Consulta el estado de tu reembolso en «Mis pagos». Proyecto académico sin validez fiscal."));
    }

    /** Aviso del reembolso: rechazado o dinero devuelto. El mensaje llega ya escapado desde el servicio. */
    public void avisoReembolso(String correo, String nombre, String asunto, String mensajeHtml) {
        enviar(correo, asunto + " · BioLearn",
                plantilla("¡Hola, " + nombre + "!", "<p>" + mensajeHtml + "</p>", null, null,
                        "Consulta el detalle en «Mis pagos» de BioLearn."));
    }

    // ── envío ───────────────────────────────────────────────────────────────

    /** Envía en segundo plano; sin configuración, solo deja el aviso en la consola. */
    public void enviar(String para, String asunto, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!habilitado || sender == null || remitente.isBlank()) {
            log.info("Correo no enviado (envío desactivado) para {}: {}", para, asunto);
            return;
        }
        String destino = redirigirA.isEmpty() ? para : redirigirA;
        String asuntoFinal = redirigirA.isEmpty() ? asunto : "[para " + para + "] " + asunto;
        envios.submit(() -> {
            try {
                MimeMessage m = sender.createMimeMessage();
                MimeMessageHelper h = new MimeMessageHelper(m, false, "UTF-8");
                h.setFrom(remitente, "BioLearn");
                h.setTo(destino);
                h.setSubject(asuntoFinal);
                h.setText(html, true);
                sender.send(m);
                log.info("Correo enviado a {} ({}): {}", destino, para, asunto);
            } catch (Exception e) {
                log.warn("No se pudo enviar el correo a {} ({}): {}", destino, asunto, e.getMessage());
            }
        });
    }

    static String plantilla(String saludo, String cuerpo, String boton, String enlace, String nota) {
        String btn = boton == null ? "" :
                "<p style=\"margin:22px 0\"><a href=\"" + enlace + "\" style=\"background:#2C4A42;color:#fff;padding:12px 22px;"
                        + "border-radius:8px;text-decoration:none;font-weight:bold\">" + esc(boton) + "</a></p>"
                        + "<p style=\"font-size:12px;color:#777\">Si el botón no funciona, copia este enlace en el navegador:<br>"
                        + "<span style=\"word-break:break-all\">" + esc(enlace) + "</span></p>";
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:auto;color:#2A2118\">"
                + "<div style=\"border-bottom:3px solid #2C4A42;padding:14px 0;font-size:22px;font-weight:800;color:#2C4A42\">🌿 BioLearn</div>"
                + "<h2 style=\"font-size:18px;margin:20px 0 10px\">" + esc(saludo) + "</h2>" + cuerpo + btn
                + "<p style=\"font-size:12px;color:#8A6A4E;border-top:1px solid #E0E2E8;padding-top:12px;margin-top:24px\">" + esc(nota) + "</p></div>";
    }

    private static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }
}
