package co.biolearn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita las tareas programadas (revisión diaria de los plazos de acceso a los cursos). */
@Configuration
@EnableScheduling
public class ProgramacionConfig {
}
