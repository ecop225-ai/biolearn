package co.biolearn.service;

import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CorreoService — envío de correos por SMTP (Pagos, Fase 1)")
class CorreoServiceTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
    private final JavaMailSender sender = mock(JavaMailSender.class);

    private CorreoService servicio(boolean habilitado, String redirigir) {
        when(proveedor.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage((jakarta.mail.Session) null));
        return new CorreoService(proveedor, habilitado, "biolearn.notificaciones@gmail.com", redirigir, "http://front/auth.html");
    }

    @Test @DisplayName("Desactivado (valor por defecto): no se envía nada")
    void desactivado() {
        servicio(false, "").activacionEstudiante("a@b.co", "Ana", "tok", 24);
        verify(sender, never()).send(any(MimeMessage.class));
    }

    @Test @DisplayName("Activado: envía el enlace de activación al destinatario")
    void envia() throws Exception {
        servicio(true, "").activacionEstudiante("a@b.co", "Ana", "tok-123", 24);
        ArgumentCaptor<MimeMessage> m = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, timeout(3000)).send(m.capture());
        assertThat(m.getValue().getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("a@b.co");
        assertThat(m.getValue().getSubject()).isEqualTo("Activa tu cuenta de BioLearn");
        assertThat(String.valueOf(m.getValue().getContent())).contains("http://front/auth.html?verificar=tok-123");
    }

    @Test @DisplayName("Redirigir: todo llega al buzón de desarrollo con el destinatario original en el asunto")
    void redirigir() throws Exception {
        servicio(true, "pruebas@gmail.com").recuperacion("estudiante.pruebas@biolearn.co", "Esteban", "r-1", 30);
        ArgumentCaptor<MimeMessage> m = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, timeout(3000)).send(m.capture());
        assertThat(m.getValue().getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("pruebas@gmail.com");
        assertThat(m.getValue().getSubject()).startsWith("[para estudiante.pruebas@biolearn.co]");
        assertThat(String.valueOf(m.getValue().getContent())).contains("?recuperar=r-1");
    }

    @Test @DisplayName("La plantilla escapa el HTML de los datos")
    void plantilla() {
        assertThat(CorreoService.plantilla("<b>Ana</b>", "<p>x</p>", "Ir", "http://x", "nota"))
                .contains("&lt;b&gt;Ana&lt;/b&gt;").contains("<p>x</p>").contains("href=\"http://x\"");
    }
}
