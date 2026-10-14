package sn.anaqsup.portail.shared;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration propre à l'application (préfixe {@code anaqsup.*}).
 *
 * @param corsAllowedOrigins origines autorisées à appeler l'API depuis un navigateur
 *                           (portail, back-office)
 */
@ConfigurationProperties(prefix = "anaqsup")
public record AnaqsupProperties(List<String> corsAllowedOrigins) {

    public AnaqsupProperties {
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
    }
}
