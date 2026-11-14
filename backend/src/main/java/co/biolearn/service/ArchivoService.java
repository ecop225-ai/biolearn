package co.biolearn.service;

import co.biolearn.dto.ArchivoResponse;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface ArchivoService {

    ArchivoResponse guardar(MultipartFile archivo);

    Resource cargar(String nombre);

    String tipoContenido(String nombre);
}
