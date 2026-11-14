package co.biolearn.service;

import co.biolearn.dto.PagoPasarelaRequest;
import co.biolearn.model.Pago;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Pasarela de pruebas con los mismos datos del sandbox de Wompi:
 * - Tarjeta: 4242 4242 4242 4242 aprueba; 4111 1111 1111 1111 rechaza.
 * - Nequi: 3991111111 aprueba; 3992222222 rechaza.
 * - PSE: banco "1" aprueba; banco "2" rechaza.
 * Cualquier otro dato es inválido (400), para que la prueba sea explícita.
 */
@Component
public class PasarelaPruebas implements PasarelaPago {

    static final String TARJETA_APRUEBA = "4242424242424242";
    static final String TARJETA_RECHAZA = "4111111111111111";
    static final String NEQUI_APRUEBA = "3991111111";
    static final String NEQUI_RECHAZA = "3992222222";

    @Override
    public Resultado cobrar(Pago pago, PagoPasarelaRequest datos) {
        String ref = "PRUEBA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        PagoPasarelaRequest d = datos == null ? new PagoPasarelaRequest() : datos;
        return switch (pago.getMedioPago()) {
            case "wompi_tarjeta" -> {
                String n = soloDigitos(d.getNumeroTarjeta());
                if (TARJETA_APRUEBA.equals(n)) yield Resultado.aprobado(ref);
                if (TARJETA_RECHAZA.equals(n)) yield Resultado.rechazado(ref, "La entidad financiera rechazó la tarjeta");
                throw new IllegalArgumentException("Usa una tarjeta de prueba: 4242 4242 4242 4242 (aprobada) o 4111 1111 1111 1111 (rechazada)");
            }
            case "wompi_nequi" -> {
                String t = soloDigitos(d.getTelefono());
                if (NEQUI_APRUEBA.equals(t)) yield Resultado.aprobado(ref);
                if (NEQUI_RECHAZA.equals(t)) yield Resultado.rechazado(ref, "El pago no fue aceptado en Nequi");
                throw new IllegalArgumentException("Usa un número Nequi de prueba: 3991111111 (aprobado) o 3992222222 (rechazado)");
            }
            case "pse" -> {
                String b = d.getBanco() == null ? "" : d.getBanco().trim();
                if ("1".equals(b)) yield Resultado.aprobado(ref);
                if ("2".equals(b)) yield Resultado.rechazado(ref, "El banco no autorizó el débito");
                throw new IllegalArgumentException("Elige un banco de prueba: 1 (aprobado) o 2 (rechazado)");
            }
            default -> throw new IllegalArgumentException("Este medio de pago no se procesa en la pasarela");
        };
    }

    private static String soloDigitos(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }
}
