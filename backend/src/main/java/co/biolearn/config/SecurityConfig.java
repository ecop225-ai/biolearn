package co.biolearn.config;

import co.biolearn.security.CustomUserDetailsService;
import co.biolearn.security.JwtAuthenticationEntryPoint;
import co.biolearn.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize("hasRole('ADMIN')") en los controladores
@RequiredArgsConstructor
public class SecurityConfig {

    /** Límite de subida de 11 MB en el servidor; el servicio rechaza con un mensaje claro lo que supere 10 MB.
     *  Se define aquí para no depender de spring.servlet.multipart.* en application.properties (por defecto, 1 MB). */
    @Bean
    public jakarta.servlet.MultipartConfigElement multipartConfigElement() {
        org.springframework.boot.web.servlet.MultipartConfigFactory f = new org.springframework.boot.web.servlet.MultipartConfigFactory();
        f.setMaxFileSize(org.springframework.util.unit.DataSize.ofMegabytes(11));
        f.setMaxRequestSize(org.springframework.util.unit.DataSize.ofMegabytes(12));
        return f.createMultipartConfig();
    }

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Value("${biolearn.cors.allowed-origin}")
    private String allowedOrigin;

    /** Endpoints que cualquiera puede llamar sin token: login, registro, recuperación, activación, y la documentación.
     *  OJO: /api/auth/me y todo /api/usuarios/** NO están aquí a propósito — requieren autenticación. */
    private static final String[] RUTAS_PUBLICAS = {
            "/api/auth/login",
            "/api/auth/registro",
            "/api/auth/recuperar-password",
            "/api/auth/restablecer-password",
            "/api/auth/verificar-cuenta",
            "/api/auth/completar-invitacion",
            "/api/auth/reenviar-verificacion",
            "/api/auth/validar-codigo-institucion",
            "/api/catalogos/grados",   // solo GET; la administración está en /api/catalogos/admin/** (requiere ADMIN)
            "/api/catalogos/areas",
            "/api/cursos/catalogo",    // catálogo público: solo cursos activos (la gestión está en /api/cursos/**, requiere ADMIN)
            "/api/cursos/catalogo/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error" // sin esto, un 404 o 500 interno se enmascara como 401 al redirigir a /error
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // API sin estado (JWT), no usa cookies de sesión
            // Los PDF e imágenes subidos se muestran dentro del frontend (vista «como estudiante»).
            // En lugar de X-Frame-Options: DENY (nadie puede incrustar), solo el frontend de BioLearn puede hacerlo;
            // cualquier otro sitio sigue bloqueado (protección contra clickjacking).
            .headers(h -> h
                .frameOptions(f -> f.disable())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "frame-ancestors 'self' " + allowedOrigin + " http://localhost:5173 http://127.0.0.1:5173")))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh.authenticationEntryPoint(jwtAuthenticationEntryPoint)) // sin token / token inválido → 401
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(RUTAS_PUBLICAS).permitAll()
                    // Descarga pública de archivos de los cursos (la subida, POST, sí exige token y rol)
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/archivos/**").permitAll()
                    .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
