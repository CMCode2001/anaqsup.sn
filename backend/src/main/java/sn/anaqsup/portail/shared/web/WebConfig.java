package sn.anaqsup.portail.shared.web;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import sn.anaqsup.portail.shared.AnaqsupProperties;

@Configuration(proxyBeanMethods = false)
class WebConfig {

    /**
     * ETag sur les lectures publiques : un client (ou Nginx) qui revalide reçoit un 304
     * sans corps. Combiné au {@code Cache-Control} posé par chaque contrôleur.
     */
    @Bean
    FilterRegistrationBean<ShallowEtagHeaderFilter> publicEtagFilter() {
        var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/v1/public/*");
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AnaqsupProperties properties) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(properties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "If-None-Match"));
        config.setExposedHeaders(List.of("ETag", "Retry-After"));
        config.setMaxAge(Duration.ofHours(1));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
