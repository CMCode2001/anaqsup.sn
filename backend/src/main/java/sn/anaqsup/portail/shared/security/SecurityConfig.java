package sn.anaqsup.portail.shared.security;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Politique d'accès de l'API (ADR-0006).
 * <ul>
 *   <li>{@code GET /api/v1/public/**} : anonyme (site institutionnel) ;</li>
 *   <li>{@code /api/v1/admin/**} : chaîne dédiée, prioritaire, du module {@code comptes}
 *       (connexion par session, CSRF, rôles) ;</li>
 *   <li>{@code /api/v1/portal/**} : appels du portail (votes des lecteurs), signés ({@link PortalSignature}) ;
 *       chemin interne, jamais exposé par Nginx ;</li>
 *   <li>tout le reste : refusé par défaut.</li>
 * </ul>
 * L'actuator écoute sur un port de management distinct, jamais exposé par Nginx.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, Clock clock,
                                    @Value("${anaqsup.portal.revalidate-secret:}") String portalSecret) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/public/**").permitAll()
                        .requestMatchers("/api/v1/portal/**").access(new PortalSignature(portalSecret, clock))
                        .requestMatchers(HttpMethod.GET,
                                "/openapi/**", "/docs", "/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**", "/error").permitAll()
                        .anyRequest().denyAll())
                // Requête non authentifiée vers une ressource protégée : 401 (et non 403).
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
