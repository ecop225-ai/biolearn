package co.biolearn.service.impl;

import co.biolearn.dto.ArchivoResponse;
import co.biolearn.exception.RecursoNoEncontradoException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ArchivoServiceImpl — subida de archivos con validación del tipo real (RNF-005)")
class ArchivoServiceImplTest {

    @TempDir Path carpeta;
    private ArchivoServiceImpl servicio;

    private static final byte[] PDF = "%PDF-1.7 contenido".getBytes();
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    @BeforeEach
    void preparar() { servicio = new ArchivoServiceImpl(carpeta.toString()); }

    @Test @DisplayName("PDF, PNG y JPG válidos → se guardan con nombre UUID y URL /api/archivos/…")
    void tiposValidos() throws Exception {
        ArchivoResponse pdf = servicio.guardar(new MockMultipartFile("archivo", "guia.pdf", "application/pdf", PDF));
        ArchivoResponse png = servicio.guardar(new MockMultipartFile("archivo", "celula.png", "image/png", PNG));
        ArchivoResponse jpg = servicio.guardar(new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", JPG));
        assertThat(pdf.getUrl()).matches("/api/archivos/[0-9a-f-]{36}\\.pdf");
        assertThat(png.getUrl()).endsWith(".png");
        assertThat(jpg.getUrl()).endsWith(".jpg");
        assertThat(pdf.getNombreOriginal()).isEqualTo("guia.pdf");
        try (var archivos = Files.list(carpeta)) { assertThat(archivos.count()).isEqualTo(3); }
    }

    @Test @DisplayName("Un ejecutable renombrado a .pdf → 400 (se valida el contenido, no la extensión)")
    void extensionFalsa() {
        byte[] exe = {'M', 'Z', (byte) 0x90, 0, 3, 0};
        assertThatThrownBy(() -> servicio.guardar(new MockMultipartFile("archivo", "tarea.pdf", "application/pdf", exe)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("solo PDF, PNG o JPG");
    }

    @Test @DisplayName("Archivo vacío → 400")
    void vacio() {
        assertThatThrownBy(() -> servicio.guardar(new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("El archivo está vacío");
    }

    @Test @DisplayName("Archivo de más de 10 MB → 400")
    void demasiadoGrande() {
        byte[] grande = new byte[(int) ArchivoServiceImpl.TAMANO_MAXIMO + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);
        assertThatThrownBy(() -> servicio.guardar(new MockMultipartFile("archivo", "grande.pdf", "application/pdf", grande)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("10 MB");
    }

    @Test @DisplayName("Descargar un archivo guardado → existe; nombre inválido o inexistente → 404")
    void descargar() {
        String nombre = servicio.guardar(new MockMultipartFile("archivo", "g.pdf", "application/pdf", PDF)).getUrl().replace("/api/archivos/", "");
        assertThat(servicio.cargar(nombre).exists()).isTrue();
        assertThat(servicio.tipoContenido(nombre)).isEqualTo("application/pdf");
        assertThatThrownBy(() -> servicio.cargar("../../application.properties")).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.cargar("123e4567-e89b-12d3-a456-426614174000.pdf")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
