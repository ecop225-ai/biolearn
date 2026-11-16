package co.biolearn.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte financiero listo para mostrar (vista imprimible / PDF) o para escribir en Excel: indicadores y tablas
 * con columnas tipadas (texto, numero, moneda, porcentaje, fecha). Los valores de las filas son texto, números o fechas.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReporteFinancieroResponse {
    private Integer idRegistro;
    private String tipo;
    private String titulo;
    private LocalDate desde;
    private LocalDate hasta;
    private LocalDateTime generadoEn;
    private String generadoPor;
    private String motivo;
    private String razonSocial;
    private String nit;
    private List<Indicador> indicadores;
    private List<Tabla> tablas;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Indicador {
        private String etiqueta;
        private Object valor;
        /** moneda | numero | porcentaje */
        private String formato;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Columna {
        private String nombre;
        /** texto | numero | moneda | porcentaje | fecha */
        private String tipo;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Tabla {
        private String titulo;
        /** true: tabla de detalle (hoja propia en Excel); false: tabla de resumen. */
        private boolean detalle;
        private List<Columna> columnas;
        private List<List<Object>> filas;
        /** Fila de totales (puede ser null). */
        private List<Object> total;
        private String nota;
    }
}
