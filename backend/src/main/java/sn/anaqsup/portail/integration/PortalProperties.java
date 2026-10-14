package sn.anaqsup.portail.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Webhook de revalidation du portail ({@code anaqsup.portal.*}).
 * Sans adresse, aucun appel n'est fait (le portail se met à jour à l'expiration de son cache, 10 minutes au plus).
 */
@ConfigurationProperties(prefix = "anaqsup.portal")
record PortalProperties(String revalidateUrl, String revalidateSecret) {

    boolean enabled() {
        return revalidateUrl != null && !revalidateUrl.isBlank();
    }
}
