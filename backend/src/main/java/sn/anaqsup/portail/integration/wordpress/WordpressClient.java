package sn.anaqsup.portail.integration.wordpress;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lecture de l'ancien site : pages, flux et fichiers, avec une identité claire, des délais bornés et
 * deux nouvelles tentatives en cas d'erreur réseau ou serveur (5xx).
 */
final class WordpressClient {

    /** Ressource absente (404) : fin du flux paginé, image supprimée… */
    static final class NotFound extends IOException {
        NotFound(URI uri) {
            super("Introuvable : " + uri);
        }
    }

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; ANAQ-Sup-reprise/1.0; +https://anaqsup.sn)";
    private static final int ATTEMPTS = 3;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    String text(String url) throws IOException {
        return new String(bytes(url, 20 * 1024 * 1024), StandardCharsets.UTF_8);
    }

    /** Contenu complet, refusé au-delà de {@code maxBytes} (sans tout télécharger). */
    byte[] bytes(String url, int maxBytes) throws IOException {
        URI uri = URI.create(WordpressParsing.encode(url));
        IOException last = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                HttpResponse<InputStream> response = http.send(HttpRequest.newBuilder(uri)
                        .header("User-Agent", USER_AGENT)
                        .timeout(Duration.ofSeconds(60))
                        .GET().build(), HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream body = response.body()) {
                    int status = response.statusCode();
                    if (status == 404 || status == 410) {
                        throw new NotFound(uri);
                    }
                    if (status >= 500) {
                        last = new IOException("Erreur " + status + " sur " + uri);
                        continue;
                    }
                    if (status != 200) {
                        throw new IOException("Réponse " + status + " sur " + uri);
                    }
                    long declared = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                    if (declared > maxBytes) {
                        throw new TooLarge(uri, declared);
                    }
                    byte[] content = body.readNBytes(maxBytes + 1);
                    if (content.length > maxBytes) {
                        throw new TooLarge(uri, content.length);
                    }
                    return content;
                }
            } catch (NotFound | TooLarge e) {
                throw e;
            } catch (IOException e) {
                last = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrompu", e);
            }
            sleep(attempt);
        }
        throw last;
    }

    static final class TooLarge extends IOException {
        TooLarge(URI uri, long size) {
            super("Fichier trop lourd (" + size / 1024 + " Ko) : " + uri);
        }
    }

    private static void sleep(int attempt) {
        try {
            Thread.sleep(Duration.ofSeconds(2L * attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
