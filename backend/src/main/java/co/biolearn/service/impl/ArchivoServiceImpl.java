package co.biolearn.service.impl;

import co.biolearn.dto.ArchivoResponse;
import co.biolearn.exception.RecursoNoEncontradoException;
import co.biolearn.service.ArchivoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Archivos de los cursos (RNF-005): PDF, PNG o JPG de máximo 10 MB.
 * El tipo se valida por la firma interna del archivo (sus primeros bytes), no por la extensión ni por el
 * Content-Type que envía el navegador. Se guarda con un nombre aleatorio (UUID) en la carpeta configurada
 * con biolearn.archivos.ruta (por defecto, ./uploads junto al backend).
 */
@Service
public class ArchivoServiceImpl implements ArchivoService {

    static final long TAMANO_MAXIMO = 10L * 1024 * 1024;
    private static final String NOMBRE_VALIDO = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png|jpg)$";

    private final Path carpeta;

    public ArchivoServiceImpl(@Value("${biolearn.archivos.ruta:uploads}") String ruta) {
        this.carpeta = Paths.get(ruta).toAbsolutePath().normalize();
        org.slf4j.LoggerFactory.getLogger(ArchivoServiceImpl.class).info("Archivos de los cursos en: {}", this.carpeta);
    }

    @Override
    public ArchivoResponse guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new IllegalArgumentException("El archivo está vacío");
        if (archivo.getSize() > TAMANO_MAXIMO) throw new IllegalArgumentException("El archivo supera el tamaño máximo de 10 MB");
        byte[] cabecera = new byte[8];
        int leidos;
        try (InputStream in = archivo.getInputStream()) {
            leidos = in.readNBytes(cabecera, 0, cabecera.length);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
        String extension = detectarTipo(cabecera, leidos);
        if (extension == null) {
            throw new IllegalArgumentException("Tipo de archivo no permitido: solo PDF, PNG o JPG (se valida el contenido real del archivo, no su extensión)");
        }
        String nombre = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(carpeta);
            try (InputStream in = archivo.getInputStream()) {
                Files.copy(in, carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return ArchivoResponse.builder().url("/api/archivos/" + nombre)
                .nombreOriginal(archivo.getOriginalFilename()).tipo(tipoContenido(nombre)).tamanoBytes(archivo.getSize()).build();
    }

    /** Firma interna: %PDF → pdf; 89 50 4E 47 0D 0A 1A 0A → png; FF D8 FF → jpg. */
    static String detectarTipo(byte[] b, int n) {
        if (n >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return "png";
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        return null;
    }

    @Override
    public Resource cargar(String nombre) {
        // El patrón impide rutas como ../../ (solo nombres UUID generados por el sistema)
        if (nombre == null || !nombre.matches(NOMBRE_VALIDO)) throw new RecursoNoEncontradoException("El archivo no existe");
        Path ruta = carpeta.resolve(nombre).normalize();
        if (!ruta.startsWith(carpeta) || !Files.exists(ruta)) throw new RecursoNoEncontradoException("El archivo no existe");
        return new PathResource(ruta);
    }

    @Override
    public String tipoContenido(String nombre) {
        if (nombre.endsWith(".pdf")) return "application/pdf";
        if (nombre.endsWith(".png")) return "image/png";
        return "image/jpeg";
    }
}
