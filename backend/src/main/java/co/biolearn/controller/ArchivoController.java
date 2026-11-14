package co.biolearn.controller;

import co.biolearn.dto.ArchivoResponse;
import co.biolearn.service.ArchivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Archivos de los cursos (RNF-005). La subida exige rol DOCENTE o ADMIN; la descarga es pública porque las imágenes
 * y los PDF se muestran con etiquetas <img> y enlaces, que no envían el token (el nombre UUID impide adivinarlos).
 */
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
@Tag(name = "Archivos", description = "Subida (PDF, PNG o JPG de máximo 10 MB) y descarga de archivos de los cursos")
public class ArchivoController {

    private final ArchivoService archivoService;
    private final co.biolearn.service.ArchivoFirma archivoFirma;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DOCENTE','ADMIN','ESTUDIANTE')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Sube un PDF, PNG o JPG de máximo 10 MB (docentes y Administrador para los cursos; estudiantes para el comprobante de pago); el tipo se valida por el contenido real del archivo")
    public ResponseEntity<ArchivoResponse> subir(@RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(archivoService.guardar(archivo));
    }

    @GetMapping("/{nombre}")
    @Operation(summary = "Descarga un archivo subido. Las imágenes son públicas; los PDF exigen el enlace firmado (exp y firma) que entrega la API")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre,
                                              @RequestParam(required = false) Long exp,
                                              @RequestParam(required = false) String firma) {
        if (archivoFirma.requiereFirma(nombre) && !archivoFirma.valida(nombre, exp, firma)) {
            throw new co.biolearn.exception.SinPermisoException("El enlace del documento no es válido o venció; vuelve a abrirlo desde la plataforma");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivoService.tipoContenido(nombre)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .body(archivoService.cargar(nombre));
    }
}
