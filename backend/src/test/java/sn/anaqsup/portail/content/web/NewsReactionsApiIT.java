package sn.anaqsup.portail.content.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import sn.anaqsup.portail.support.AdminBrowser;

/**
 * Mentions « J'aime » / « Je n'aime pas » (décision ANAQ-Sup du 14/10/2026) : appels signés du portail, un vote
 * par navigateur, changement d'avis, « Je n'aime pas » jamais publics, compteurs dans le back-office.
 */
@SpringBootTest(properties = {
        "anaqsup.admin.bootstrap.email=admin@anaqsup.test",
        "anaqsup.admin.bootstrap.password=provisoire-1",
        "anaqsup.portal.revalidate-secret=" + NewsReactionsApiIT.SECRET,
})
@AutoConfigureMockMvc
@Import(NewsReactionsApiIT.Containers.class)
class NewsReactionsApiIT {

    static final String SECRET = "secret-de-test-portail-0000000000000000";
    private static final String A = "3f2b8c1e-5a4d-4e6f-9b7a-1c2d3e4f5a6b";
    private static final String B = "9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    @TempDir
    static Path media;

    @DynamicPropertySource
    static void mediaDir(DynamicPropertyRegistry registry) {
        registry.add("anaqsup.media.dir", () -> media.toString());
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ApplicationRunner adminBootstrap;

    private AdminBrowser admin;

    @BeforeEach
    void donnees() throws Exception {
        jdbc.sql("DELETE FROM admin_account").update();
        adminBootstrap.run(null);
        jdbc.sql("DELETE FROM news_item").update();
        admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
        assertThat(admin.post("/news", article("Une actualité appréciée", true))).hasStatus(HttpStatus.CREATED);
        assertThat(admin.post("/news", article("Un brouillon", false))).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void unVoteParNavigateurModifiableEtJeNaimePasJamaisPublic() {
        String path = "/api/v1/portal/news/une-actualite-appreciee/reactions";
        assertThat(signed(HttpMethod.PUT, path + "/like", A)).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"likes": 1, "mine": "LIKE"}""");
        // Voter deux fois « J'aime » ne compte qu'une fois.
        assertThat(signed(HttpMethod.PUT, path + "/like", A)).bodyJson().extractingPath("$.likes").isEqualTo(1);
        assertThat(signed(HttpMethod.PUT, path + "/like", B)).bodyJson().extractingPath("$.likes").isEqualTo(2);

        // Changement d'avis : le « J'aime » devient « Je n'aime pas », dont le nombre n'est jamais renvoyé.
        var changed = signed(HttpMethod.PUT, path + "/dislike", A);
        assertThat(changed).bodyJson().isLenientlyEqualTo("""
                {"likes": 1, "mine": "DISLIKE"}""");
        assertThat(changed).bodyJson().doesNotHavePath("$.dislikes");
        assertThat(signed(HttpMethod.GET, path, B)).bodyJson().extractingPath("$.mine").isEqualTo("LIKE");
        assertThat(signed(HttpMethod.GET, path, null)).bodyJson().extractingPath("$.mine").isNull();

        // Le back-office voit les deux compteurs.
        var list = admin.get("/news");
        assertThat(list).bodyJson().extractingPath("$.content[?(@.slug == 'une-actualite-appreciee')].likes").asArray().containsExactly(1);
        assertThat(list).bodyJson().extractingPath("$.content[?(@.slug == 'une-actualite-appreciee')].dislikes").asArray().containsExactly(1);

        // Annulation.
        assertThat(signed(HttpMethod.PUT, path + "/none", B)).bodyJson().isLenientlyEqualTo("""
                {"likes": 0}""");

        // Seule l'empreinte de l'identifiant du navigateur est enregistrée.
        assertThat(jdbc.sql("SELECT count(*) FROM news_reaction WHERE position(convert_to(:raw, 'UTF8') IN visitor) > 0")
                .param("raw", A).query(Integer.class).single()).isZero();
    }

    @Test
    void seulLePortailPeutVoterEtSeulementSurUneActualitePubliee() {
        String path = "/api/v1/portal/news/une-actualite-appreciee/reactions/like";
        assertThat(mvc.put().uri(path).header("X-Anaqsup-Visitor", A)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(signed(HttpMethod.PUT, path, A, Instant.now().minusSeconds(600), SECRET)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(signed(HttpMethod.PUT, path, A, Instant.now(), "autre-secret-0000000000000000000000000")).hasStatus(HttpStatus.UNAUTHORIZED);
        // Signature d'un autre navigateur : refusée (le visiteur fait partie de la signature).
        assertThat(mvc.put().uri(path).header("X-Anaqsup-Visitor", B)
                .headers(h -> signature(HttpMethod.PUT, path, A, Instant.now(), SECRET).forEach(h::add)))
                .hasStatus(HttpStatus.UNAUTHORIZED);

        assertThat(signed(HttpMethod.PUT, "/api/v1/portal/news/un-brouillon/reactions/like", A)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(signed(HttpMethod.PUT, path, "pas-un-identifiant")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.sql("SELECT count(*) FROM news_reaction").query(Integer.class).single()).isZero();
    }

    private MvcTestResult signed(HttpMethod method, String path, @Nullable String visitor) {
        return signed(method, path, visitor, Instant.now(), SECRET);
    }

    private MvcTestResult signed(HttpMethod method, String path, @Nullable String visitor, Instant at, String secret) {
        var request = mvc.method(method).uri(path).headers(h -> signature(method, path, visitor, at, secret).forEach(h::add));
        if (visitor != null) {
            request = request.header("X-Anaqsup-Visitor", visitor);
        }
        return request.exchange();
    }

    /** Signature calculée comme le portail (src/lib/reactions.ts). */
    private static Map<String, String> signature(HttpMethod method, String path, @Nullable String visitor, Instant at, String secret) {
        String timestamp = String.valueOf(at.getEpochSecond());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + method.name() + "." + path + "." + (visitor == null ? "" : visitor))
                    .getBytes(StandardCharsets.UTF_8));
            return Map.of("X-Anaqsup-Timestamp", timestamp, "X-Anaqsup-Signature", "sha256=" + HexFormat.of().formatHex(digest));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String article(String title, boolean published) {
        return """
                {"title": "%s", "bodyHtml": "<p>Texte</p>", "category": "vie-institutionnelle", "published": %s,
                 "publishedAt": "2026-10-01T10:00:00Z", "featured": false}""".formatted(title, published);
    }
}
