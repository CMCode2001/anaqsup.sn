package sn.anaqsup.portail.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.json.JsonMapper;

import sn.anaqsup.portail.content.MachineTranslator;

/** Appel de l'API DeepL v2, contre un faux serveur local. */
class DeeplTranslatorTest {

    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> request = new AtomicReference<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void traduitEnAnglaisBritanniqueEnConservantLeHtml() throws IOException {
        var translator = translator(200, """
                {"translations": [{"detected_source_language": "FR", "text": "<p>The <strong>programme</strong></p>"}]}""");

        assertThat(translator.toEnglish(List.of("<p>Le <strong>programme</strong></p>"), true))
                .containsExactly("<p>The <strong>programme</strong></p>");
        assertThat(authorization.get()).isEqualTo("DeepL-Auth-Key cle-test:fx");
        assertThat(request.get()).contains("\"source_lang\":\"FR\"", "\"target_lang\":\"EN-GB\"", "\"tag_handling\":\"html\"");
    }

    @Test
    void quotaAtteintSignaleCommeServiceIndisponible() throws IOException {
        var translator = translator(456, "{\"message\": \"Quota exceeded\"}");

        assertThatThrownBy(() -> translator.toEnglish(List.of("Bonjour"), false))
                .isInstanceOf(MachineTranslator.UnavailableException.class)
                .hasMessageContaining("quota");
    }

    @Test
    void sansCleRienNEstEnvoye() {
        var translator = new DeeplTranslator(new DeeplProperties("", null), JsonMapper.builder().build());
        assertThat(translator.available()).isFalse();
        assertThatThrownBy(() -> translator.toEnglish(List.of("Bonjour"), false))
                .isInstanceOf(MachineTranslator.UnavailableException.class);
    }

    @Test
    void cleGratuiteVersLApiGratuite() {
        assertThat(new DeeplProperties("abc:fx", null).endpoint()).hasHost("api-free.deepl.com");
        assertThat(new DeeplProperties("abc", null).endpoint()).hasHost("api.deepl.com");
    }

    private DeeplTranslator translator(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/translate", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v2/translate");
        return new DeeplTranslator(new DeeplProperties("cle-test:fx", url), JsonMapper.builder().build());
    }
}
