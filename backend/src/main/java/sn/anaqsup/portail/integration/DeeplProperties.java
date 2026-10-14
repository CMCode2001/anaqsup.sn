package sn.anaqsup.portail.integration;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Traduction automatique DeepL ({@code anaqsup.translation.*}). Sans clé, rien n'est envoyé à DeepL et les
 * contenus non traduits restent en français sur le site anglais.
 *
 * @param deeplApiKey clé d'API ; une clé de l'offre gratuite se termine par « :fx »
 * @param deeplUrl    adresse de l'API, déduite de la clé si absente (utile pour les tests)
 */
@ConfigurationProperties(prefix = "anaqsup.translation")
record DeeplProperties(@Nullable String deeplApiKey, @Nullable URI deeplUrl) {

    boolean enabled() {
        return deeplApiKey != null && !deeplApiKey.isBlank();
    }

    URI endpoint() {
        if (deeplUrl != null) {
            return deeplUrl;
        }
        boolean free = deeplApiKey != null && deeplApiKey.strip().endsWith(":fx");
        return URI.create(free ? "https://api-free.deepl.com/v2/translate" : "https://api.deepl.com/v2/translate");
    }
}
