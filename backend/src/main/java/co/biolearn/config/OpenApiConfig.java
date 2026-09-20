package co.biolearn.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita el botón "Authorize" en Swagger para probar endpoints protegidos con JWT.
 * Al pegar "Bearer TU_TOKEN" ahí, Swagger lo agrega automáticamente
 * al header Authorization de cada petición que hagas desde la interfaz.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "BioLearn API", version = "v1", description = "API REST de la plataforma educativa BioLearn")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
