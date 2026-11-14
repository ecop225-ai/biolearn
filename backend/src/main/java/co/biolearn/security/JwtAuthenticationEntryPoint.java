package co.biolearn.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Responde 401 (no 403) cuando se llama a un endpoint protegido sin token, con un token
 * expirado/corrupto, o con el token de una cuenta que ya no está activa. Así el frontend
 * puede distinguir "vuelve a iniciar sesión" (401) de "no tienes permiso" (403).
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"fecha\":\"" + LocalDateTime.now() + "\","
                + "\"estadoHttp\":401,"
                + "\"mensaje\":\"Tu sesión no es válida o expiró. Inicia sesión nuevamente.\","
                + "\"detalles\":null}");
    }
}
