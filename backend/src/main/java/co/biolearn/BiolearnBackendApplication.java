package co.biolearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API de BioLearn.
 * Al ejecutar este archivo (botón ▶ en el IDE, o `mvn spring-boot:run`)
 * se levanta el servidor embebido en http://localhost:8080
 */
@SpringBootApplication
public class BiolearnBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiolearnBackendApplication.class, args);
    }

}
