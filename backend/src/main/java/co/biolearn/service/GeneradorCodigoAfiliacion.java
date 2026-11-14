package co.biolearn.service;

import co.biolearn.repository.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Genera códigos de afiliación de 8 caracteres (CHAR(8) en la BD), únicos en la tabla institucion.
 * El alfabeto excluye I, O, 0 y 1 para que el código pueda dictarse o copiarse sin confusiones.
 */
@Component
@RequiredArgsConstructor
public class GeneradorCodigoAfiliacion {

    public static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    public static final int LONGITUD = 8;
    private static final int MAX_INTENTOS = 50;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InstitucionRepository institucionRepository;

    public String generarUnico() {
        for (int i = 0; i < MAX_INTENTOS; i++) {
            String codigo = generar();
            if (!institucionRepository.existsByCodigoAfiliacion(codigo)) {
                return codigo;
            }
        }
        // Con 32^8 (≈1,1 billones) combinaciones esto es prácticamente imposible; se deja como salvaguarda.
        throw new IllegalStateException("No fue posible generar un código de afiliación único");
    }

    String generar() {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
