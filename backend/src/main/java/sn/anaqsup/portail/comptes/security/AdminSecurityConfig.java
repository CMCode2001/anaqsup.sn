package sn.anaqsup.portail.comptes.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.session.web.http.DefaultCookieSerializer;

import tools.jackson.databind.json.JsonMapper;

import sn.anaqsup.portail.comptes.Permission;
import sn.anaqsup.portail.comptes.persistence.AccountRepository;
import sn.anaqsup.portail.shared.web.Problems;

/**
 * Sécurité de {@code /api/v1/admin/**} (comptes simples, ADR-0006), prioritaire sur la chaîne publique.
 * <ul>
 *   <li>Connexion par e-mail et mot de passe ; session côté serveur, cookie {@code ANAQSUP_SESSION}
 *       (HttpOnly, SameSite=Strict, Secure en production), 30 minutes d'inactivité ;</li>
 *   <li>CSRF : jeton dans le cookie {@code XSRF-TOKEN}, renvoyé par le back-office dans l'en-tête
 *       {@code X-XSRF-TOKEN} (mode « SPA » de Spring Security) ;</li>
 *   <li>mot de passe provisoire : tant qu'il n'est pas changé, seuls {@code /auth/**} répondent ;</li>
 *   <li>droits : autorités {@code PERM_*} du rôle ({@link Permission}).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class AdminSecurityConfig {

    static final String BASE = "/api/v1/admin";

    @Bean
    PasswordEncoder passwordEncoder() {
        // bcrypt par défaut, avec préfixe {id} pour pouvoir changer d'algorithme sans tout réinitialiser.
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService adminUserDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmail(email).map(AdminPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Compte inconnu"));
    }

    @Bean
    AuthenticationManager adminAuthenticationManager(UserDetailsService users, PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository adminSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Cookie de la session du back-office (sessions en base, Spring Session) : nom et attributs de
     * {@code server.servlet.session.cookie}. Déclaré ici pour s'appliquer partout : sans serveur embarqué
     * (tests MockMvc), Spring Boot ne reprendrait pas ces réglages.
     */
    @Bean
    DefaultCookieSerializer adminSessionCookie(@Value("${server.servlet.session.cookie.name}") String name,
                                               @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName(name);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite("Strict");
        serializer.setCookiePath("/");
        return serializer;
    }

    @Bean
    CookieCsrfTokenRepository adminCsrfTokenRepository() {
        var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict").path("/"));
        return repository;
    }

    /**
     * Appliquée par AuthController après une connexion réussie : nouvel identifiant de session
     * (le nouveau jeton CSRF est posé par AuthController lui-même).
     */
    @Bean
    SessionAuthenticationStrategy adminSessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    @Order(1)
    SecurityFilterChain adminSecurity(HttpSecurity http, AccountRepository accounts, SecurityContextRepository contextRepository,
                                      CookieCsrfTokenRepository csrfTokens, JsonMapper json) throws Exception {
        http
                .securityMatcher(BASE + "/**")
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens).csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                // API JSON : pas de « requête à rejouer après connexion », donc pas de session pour un visiteur anonyme.
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, BASE + "/auth/login").permitAll()
                        .requestMatchers(BASE + "/auth/**").authenticated()
                        .requestMatchers(BASE + "/accounts/**", BASE + "/roles/**").access(ready(Permission.COMPTES))
                        // Rubriques du site : chaque écran du back-office est réservé à sa permission.
                        .requestMatchers(BASE + "/pages/**").access(ready(Permission.PAGES))
                        .requestMatchers(BASE + "/partners/**").access(ready(Permission.PARTENAIRES))
                        .requestMatchers(BASE + "/publications/**", BASE + "/media/documents").access(ready(Permission.DOCUMENTS))
                        .requestMatchers(BASE + "/news/**").access(ready(Permission.ACTUALITES))
                        .requestMatchers(BASE + "/events/**").access(ready(Permission.EVENEMENTS))
                        .requestMatchers(BASE + "/registry/**").access(ready(Permission.AVIS))
                        .requestMatchers(BASE + "/settings/**").access(ready(Permission.PARAMETRES))
                        .anyRequest().access(ready(null)))
                .logout(logout -> logout
                        .logoutUrl(BASE + "/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                problem(response, json, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Connexion requise."))
                        .accessDeniedHandler((request, response, exception) -> {
                            if (exception instanceof CsrfException) {
                                problem(response, json, HttpStatus.FORBIDDEN, "CSRF",
                                        "Votre session a expiré ou la page est trop ancienne : rechargez la page.");
                                return;
                            }
                            boolean mustChange = request.getUserPrincipal() instanceof Authentication a
                                    && a.getPrincipal() instanceof AdminPrincipal p && p.mustChangePassword();
                            if (mustChange) {
                                problem(response, json, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
                                        "Changez votre mot de passe provisoire pour continuer.");
                            } else {
                                problem(response, json, HttpStatus.FORBIDDEN, "FORBIDDEN", "Votre rôle ne donne pas accès à cette rubrique.");
                            }
                        }))
                .addFilterAfter(new AdminSessionRefreshFilter(accounts, contextRepository), SecurityContextHolderFilter.class);
        return http.build();
    }

    /** Compte connecté, mot de passe provisoire changé, et permission demandée (si précisée). */
    private static AuthorizationManager<RequestAuthorizationContext> ready(@Nullable Permission permission) {
        return (Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) -> {
            Authentication auth = authentication.get();
            boolean granted = auth != null && auth.getPrincipal() instanceof AdminPrincipal principal
                    && !principal.mustChangePassword()
                    && (permission == null || principal.permissions().contains(permission));
            return new AuthorizationDecision(granted);
        };
    }

    /** Erreur RFC 9457 écrite par les filtres de sécurité (avant Spring MVC). */
    private static void problem(HttpServletResponse response, JsonMapper json, HttpStatus status, String code, String detail)
            throws IOException {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", Problems.type("admin/" + code.toLowerCase().replace('_', '-')).toString());
        problem.put("title", status == HttpStatus.UNAUTHORIZED ? "Connexion requise" : "Accès refusé");
        problem.put("status", status.value());
        problem.put("detail", detail);
        problem.put("code", code);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        json.writeValue(response.getOutputStream(), problem);
    }
}
