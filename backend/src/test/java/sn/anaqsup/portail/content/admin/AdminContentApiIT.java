package sn.anaqsup.portail.content.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.support.AdminBrowser;

/** Back-office : mot du Secrétaire exécutif (photo, légende, texte), partenaires et images. */
@SpringBootTest(properties = {
        "anaqsup.admin.bootstrap.email=admin@anaqsup.test",
        "anaqsup.admin.bootstrap.password=provisoire-1",
})
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(AdminContentApiIT.Containers.class)
class AdminContentApiIT {

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

    /** Plus petit PNG valide (1 × 1 pixel). */
    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ApplicationRunner adminBootstrap;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    void donnees() throws Exception {
        jdbc.sql("DELETE FROM admin_account").update();
        adminBootstrap.run(null);
        jdbc.sql("DELETE FROM institutional_page WHERE slug = 'mot-du-secretaire-executif'").update();
        jdbc.sql("""
                INSERT INTO institutional_page (slug, title, body_html, status, author, published_at)
                VALUES ('mot-du-secretaire-executif', 'Le mot', '<p>Ancien texte</p>', 'PUBLIE', 'test', now())""").update();
    }

    @Test
    void motDuSecretaireExecutifAvecPhotoLegendeEtTexteAssaini() throws Exception {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");

        var upload = admin.upload("/media", new MockMultipartFile("file", "photo.png", "image/png", PNG));
        assertThat(upload).hasStatus(HttpStatus.CREATED);
        String key = com.jayway.jsonpath.JsonPath.read(upload.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8), "$.key");

        assertThat(admin.put("/pages/mot-du-secretaire-executif", """
                {"title": "Le mot du Secrétaire exécutif", "summary": "Bienvenue.",
                 "bodyHtml": "<p>Texte <strong>officiel</strong><script>alert(1)</script></p><p onclick=\\"x()\\">Suite</p>",
                 "imageKey": "%s", "imageName": "Pr Prénom NOM", "imageRole": "Secrétaire exécutif de l'ANAQ-Sup",
                 "imageRoleEn": "Executive Secretary of ANAQ-Sup"}""".formatted(key)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.bodyHtml").isEqualTo("<p>Texte <strong>officiel</strong></p><p>Suite</p>");

        // Le site public affiche la photo (servie par le portail sous /media/…), le nom et la fonction traduite.
        assertThat(mvc.get().uri("/api/v1/public/pages/mot-du-secretaire-executif"))
                .bodyJson().extractingPath("$.imageUrl").isEqualTo("/media/" + key);
        var english = mvc.get().uri("/api/v1/public/pages/mot-du-secretaire-executif?lang=en").exchange();
        assertThat(english).bodyJson().extractingPath("$.imageName").isEqualTo("Pr Prénom NOM");
        assertThat(english).bodyJson().extractingPath("$.imageRole").isEqualTo("Executive Secretary of ANAQ-Sup");
        assertThat(mvc.get().uri("/api/v1/public/media/" + key)).hasStatusOk()
                .hasHeader("Cache-Control", "max-age=31536000, public, immutable")
                .hasContentType("image/png");
        // Le portail est prévenu : la page modifiée s'affiche aussitôt.
        assertThat(events.stream(ContentChanged.class)).anyMatch(e -> e.tags().contains("page:mot-du-secretaire-executif"));
    }

    @Test
    void imageRefuseeSiCeNestPasUneImageReelle() {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
        var fake = new MockMultipartFile("file", "photo.png", "image/png", "<svg onload=alert(1)>".getBytes());
        assertThat(admin.upload("/media", fake)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("UNSUPPORTED_IMAGE");
    }

    @Test
    void versionAnglaiseIncompleteRefusee() {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
        assertThat(admin.put("/pages/mot-du-secretaire-executif", """
                {"title": "Le mot", "bodyHtml": "<p>Texte</p>", "titleEn": "A word"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("ENGLISH_INCOMPLETE");
    }

    @Test
    void partenairesGeresParLAdministrateurEtListesDansLOrdre() {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");

        assertThat(admin.post("/partners", """
                {"name": "Partenaire test", "url": "https://exemple.test", "enabled": true}"""))
                .hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri("/api/v1/public/partners"))
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("MESRI", "CAMES", "CAMPUSEN", "UNESCO", "Partenaire test");

        // Nouvel ordre : le dernier passe en tête.
        var ids = jdbc.sql("SELECT CAST(id AS text) FROM partner ORDER BY position").query(String.class).list();
        var reordered = new java.util.ArrayList<>(ids);
        reordered.addFirst(reordered.removeLast());
        assertThat(admin.put("/partners/order", "{\"ids\": [\"" + String.join("\", \"", reordered) + "\"]}")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/public/partners"))
                .bodyJson().extractingPath("$[0].name").isEqualTo("Partenaire test");

        assertThat(admin.put("/partners/" + reordered.getFirst(), """
                {"name": "Partenaire test", "url": "javascript:alert(1)", "enabled": true}"""))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("INVALID_URL");
        assertThat(admin.send(HttpMethod.DELETE, "/partners/" + reordered.getFirst(), null)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void chargeDeCommunicationModifieLesPagesMaisPasLesPartenaires() {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
        String role = jdbc.sql("SELECT CAST(id AS text) FROM admin_role WHERE name = 'Chargé de communication'").query(String.class).single();
        assertThat(admin.post("/accounts", """
                {"email": "com@anaqsup.test", "displayName": "Com", "roleId": "%s", "temporaryPassword": "provisoire-2"}"""
                .formatted(role))).hasStatus(HttpStatus.CREATED);

        AdminBrowser com = AdminBrowser.login(mvc, "com@anaqsup.test", "provisoire-2", "definitif-2");
        assertThat(com.get("/pages")).hasStatusOk();
        assertThat(com.get("/partners")).hasStatus(HttpStatus.FORBIDDEN);
    }
}
