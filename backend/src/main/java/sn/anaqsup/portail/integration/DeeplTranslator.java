package sn.anaqsup.portail.integration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import sn.anaqsup.portail.content.MachineTranslator;

/**
 * Traduction français → anglais britannique (orthographe du site : programme, organisation) par l'API DeepL v2.
 * Le HTML est traduit avec {@code tag_handling=html} : balises et liens sont conservés.
 */
@Component
class DeeplTranslator implements MachineTranslator {

    private final DeeplProperties properties;
    private final JsonMapper json;
    private final RestClient http;

    DeeplTranslator(DeeplProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean available() {
        return properties.enabled();
    }

    @Override
    public List<String> toEnglish(List<String> texts, boolean html) {
        if (texts.isEmpty()) {
            return List.of();
        }
        if (!available()) {
            throw new UnavailableException("aucune clé DeepL configurée");
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("text", texts);
        request.put("source_lang", "FR");
        request.put("target_lang", "EN-GB");
        if (html) {
            request.put("tag_handling", "html");
        }
        String response;
        try {
            response = http.post().uri(properties.endpoint())
                    .header("Authorization", "DeepL-Auth-Key " + Objects.requireNonNull(properties.deeplApiKey()).strip())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json.writeValueAsString(request))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new UnavailableException(switch (e.getStatusCode().value()) {
                case 403 -> "clé DeepL refusée";
                case 456 -> "quota mensuel DeepL atteint";
                case 429 -> "trop de requêtes envoyées à DeepL";
                default -> "DeepL a répondu " + e.getStatusCode().value();
            }, e);
        } catch (ResourceAccessException e) {
            throw new UnavailableException("DeepL injoignable", e);
        }

        JsonNode translations = json.readTree(Objects.requireNonNullElse(response, "{}")).path("translations");
        if (!translations.isArray() || translations.size() != texts.size()) {
            throw new UnavailableException("réponse DeepL inattendue");
        }
        List<String> english = new ArrayList<>(texts.size());
        for (JsonNode translation : translations) {
            english.add(translation.path("text").asString());
        }
        return english;
    }
}
