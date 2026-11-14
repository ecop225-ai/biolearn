package co.biolearn.service;

import co.biolearn.repository.InstitucionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GeneradorCodigoAfiliacion — códigos de 8 caracteres")
class GeneradorCodigoAfiliacionTest {

    @Mock private InstitucionRepository institucionRepository;
    @InjectMocks private GeneradorCodigoAfiliacion generador;

    @RepeatedTest(value = 20, name = "Código generado #{currentRepetition}: 8 caracteres sin I, O, 0 ni 1")
    void formatoDelCodigo() {
        assertThat(generador.generar()).hasSize(8).matches("^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$");
    }

    @Test
    @DisplayName("Si el código ya existe, se genera otro hasta encontrar uno libre")
    void evitaCodigosRepetidos() {
        when(institucionRepository.existsByCodigoAfiliacion(anyString())).thenReturn(true, true, false);

        String codigo = generador.generarUnico();

        assertThat(codigo).hasSize(8);
        verify(institucionRepository, atLeast(3)).existsByCodigoAfiliacion(anyString());
    }

    @Test
    @DisplayName("Si nunca encuentra un código libre, falla de forma controlada")
    void agotaIntentos() {
        when(institucionRepository.existsByCodigoAfiliacion(anyString())).thenReturn(true);

        assertThatThrownBy(() -> generador.generarUnico()).isInstanceOf(IllegalStateException.class);
    }
}
