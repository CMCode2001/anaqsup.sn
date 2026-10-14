package sn.anaqsup.portail.integration;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.json.JsonMapper;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.referentiel.RegistryChanged;

/**
 * Après une modification dans le back-office (et une fois la transaction validée), demande au portail
 * de vider les caches concernés : la page modifiée s'affiche aussitôt sur anaqsup.sn.
 * Signature : {@code X-Anaqsup-Signature: sha256=<hex>} = HMAC-SHA256(secret, "timestamp.corps"),
 * avec {@code X-Anaqsup-Timestamp} (contrôlés par apps/portail/src/app/api/revalidate/route.ts).
 * Un échec est journalisé et l'événement reste à republier (registre d'événements de Spring Modulith).
 */
@Component
class PortalRevalidation {

    private static final Logger log = LoggerFactory.getLogger(PortalRevalidation.class);

    private final PortalProperties properties;
    private final JsonMapper json;
    private final RestClient http;

    PortalRevalidation(PortalProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
        // HTTP/1.1 : sur une adresse http://, le client Java tenterait sinon une montée en HTTP/2 (« h2c »)
        // que le serveur Node de Next.js refuse en coupant la connexion.
        var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @ApplicationModuleListener
    void on(ContentChanged event) {
        revalidate(event.tags());
    }

    @ApplicationModuleListener
    void on(RegistryChanged event) {
        revalidate(event.tags());
    }

    private void revalidate(java.util.List<String> tags) {
        if (!properties.enabled() || tags.isEmpty()) {
            return;
        }
        String body = json.writeValueAsString(Map.of("tags", tags));
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        http.post().uri(properties.revalidateUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Anaqsup-Timestamp", timestamp)
                .header("X-Anaqsup-Signature", "sha256=" + hmac(timestamp + "." + body))
                .body(body)
                .retrieve()
                .toBodilessEntity();
        log.info("Portail revalidé : {}", tags);
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.revalidateSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
