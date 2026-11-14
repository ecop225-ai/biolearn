package co.biolearn.service;

import co.biolearn.dto.PagoPasarelaRequest;
import co.biolearn.model.Pago;

/**
 * Pasarela de pagos en línea. La implementación actual es una pasarela de pruebas (PasarelaPruebas);
 * para producción se agrega otra implementación (p. ej. Wompi) sin cambiar el servicio de pagos.
 */
public interface PasarelaPago {

    /** Cobra el pago con los datos del medio y devuelve el resultado de la transacción. */
    Resultado cobrar(Pago pago, PagoPasarelaRequest datos);

    /** Resultado de la pasarela: aprobado o rechazado, la referencia de la transacción y el motivo del rechazo. */
    record Resultado(boolean aprobado, String referencia, String motivo) {
        public static Resultado aprobado(String referencia) { return new Resultado(true, referencia, null); }
        public static Resultado rechazado(String referencia, String motivo) { return new Resultado(false, referencia, motivo); }
    }
}
