package co.biolearn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstitucionResponse {
    private Integer id;
    private String nombre;
    private String nit;
    private String rector;
    private String direccion;
    private String telefono;
    private String correo;
    private String codigoAfiliacion;
    private Integer cuposContratados;
    /** Estudiantes con perfil activo afiliados (calculado, no almacenado). */
    private long cuposUsados;
    /** null si la institución no tiene límite de cupos. */
    private Integer cuposDisponibles;
    private String estado;
    private LocalDate fechaRegistro;
    /** Representante activo; null si la institución no tiene. */
    private RepresentanteResumen representante;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepresentanteResumen {
        private Integer idUsuario;
        private String nombreCompleto;
        private String correo;
        private String cargo;
        /** Estado de la cuenta del usuario (pendiente si aún no activó su invitación). */
        private String estadoCuenta;
    }
}
