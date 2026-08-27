package com.ticketfilms.ms_boletos.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

// Config de Resource Server todavía no existe el
// API Manager, así que por ahora ms-boletos valida el JWT directamente
// contra Google. Cuando el gateway esté listo, esta config se mantiene
// igual (defensa en profundidad) — solo cambiará el origen permitido en CORS.
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API stateless: el JWT viaja en cada request, no hay sesión de servidor
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // CSRF no aplica a APIs stateless sin cookies de sesión
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                // Health check público, útil para RNF-02 (monitoreo de disponibilidad)
                .requestMatchers("/actuator/health").permitAll()
                // toda ruta de boletos exige autenticación previa
                .requestMatchers("/api/boletos/**").authenticated()
                .anyRequest().authenticated()
                )
                // habilita la validación de JWT (Resource Server)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // registrar en logs los rechazos por 401/403
                .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, ex) -> {
                    log.warn("401 no autenticado — path={} motivo={}", request.getRequestURI(), ex.getMessage());
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED).commence(request, response, ex);
                })
                .accessDeniedHandler((request, response, ex) -> {
                    log.warn("403 acceso denegado — path={} motivo={}", request.getRequestURI(), ex.getMessage());
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                })
                );

        return http.build();
    }
}
