package co.biolearn.service;

import co.biolearn.enums.EstadoToken;
import co.biolearn.enums.RolUsuario;
import co.biolearn.exception.SolicitudInvalidaException;
import co.biolearn.model.Usuario;
import co.biolearn.model.VerificacionCuenta;
import co.biolearn.repository.VerificacionCuentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TokenVerificacionService — tokens de activación e invitación")
class TokenVerificacionServiceTest {

    @Mock private VerificacionCuentaRepository verificacionRepository;
    @InjectMocks private TokenVerificacionService tokenService;

    private Usuario usuario() {
        return Usuario.builder().id(10).correo("sofia@test.com").tipoUsuario(RolUsuario.estudiante).build();
    }

    @Test
    @DisplayName("Crear un token nuevo invalida los pendientes anteriores del mismo usuario")
    void crearTokenInvalidaAnteriores() {
        VerificacionCuenta anterior = VerificacionCuenta.builder().token("viejo").estado(EstadoToken.pendiente).build();
        when(verificacionRepository.findByUsuario_IdAndEstado(10, EstadoToken.pendiente)).thenReturn(List.of(anterior));

        VerificacionCuenta nuevo = tokenService.crearTokenActivacion(usuario(), 24, "prueba");

        assertThat(anterior.getEstado()).isEqualTo(EstadoToken.expirado);
        assertThat(nuevo.getEstado()).isEqualTo(EstadoToken.pendiente);
        assertThat(nuevo.getToken()).isNotBlank().isNotEqualTo("viejo");
        assertThat(nuevo.getExpiraEn()).isBetween(LocalDateTime.now().plusHours(23), LocalDateTime.now().plusHours(25));
        ArgumentCaptor<VerificacionCuenta> guardados = ArgumentCaptor.forClass(VerificacionCuenta.class);
        verify(verificacionRepository, atLeastOnce()).save(guardados.capture());
        assertThat(guardados.getAllValues()).contains(anterior, nuevo);
    }

    @Test
    @DisplayName("Un token inexistente es rechazado")
    void tokenInexistente() {
        when(verificacionRepository.findByToken("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tokenService.obtenerValido("nope"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El enlace de activación no es válido");
    }

    @Test
    @DisplayName("Un token ya usado o reemplazado es rechazado")
    void tokenUsado() {
        VerificacionCuenta v = VerificacionCuenta.builder().token("t").estado(EstadoToken.expirado)
                .expiraEn(LocalDateTime.now().plusHours(5)).build();
        when(verificacionRepository.findByToken("t")).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> tokenService.obtenerValido("t"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("reemplazado por uno más reciente");
    }

    @Test
    @DisplayName("Un token vencido es rechazado")
    void tokenVencido() {
        VerificacionCuenta v = VerificacionCuenta.builder().token("t").estado(EstadoToken.pendiente)
                .expiraEn(LocalDateTime.now().minusMinutes(1)).build();
        when(verificacionRepository.findByToken("t")).thenReturn(Optional.of(v));
        assertThatThrownBy(() -> tokenService.obtenerValido("t"))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessage("El enlace expiró. Solicita uno nuevo.");
    }

    @Test
    @DisplayName("Un token pendiente y vigente es aceptado")
    void tokenValido() {
        VerificacionCuenta v = VerificacionCuenta.builder().token("t").estado(EstadoToken.pendiente)
                .expiraEn(LocalDateTime.now().plusHours(1)).build();
        when(verificacionRepository.findByToken("t")).thenReturn(Optional.of(v));
        assertThat(tokenService.obtenerValido("t")).isSameAs(v);
    }

    @Test
    @DisplayName("Marcar como usado registra estado y fecha de uso")
    void marcarUsado() {
        VerificacionCuenta v = VerificacionCuenta.builder().token("t").estado(EstadoToken.pendiente).build();
        tokenService.marcarUsado(v);
        assertThat(v.getEstado()).isEqualTo(EstadoToken.usado);
        assertThat(v.getUsadoEn()).isNotNull();
        verify(verificacionRepository).save(any(VerificacionCuenta.class));
    }
}
