package sn.anaqsup.portail.content.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.referentiel.RegistryChanged;
import sn.anaqsup.portail.support.AdminBrowser;

/**
 * Back-office complet (décision ANAQ-Sup du 14/10/2026) : actualités et rubriques, événements, établissements
 * et formations, paramètres du site — et les droits de chaque rôle.
 */
@SpringBootTest(properties = {
        "anaqsup.admin.bootstrap.email=admin@anaqsup.test",
        "anaqsup.admin.bootstrap.password=provisoire-1",
})
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(AdminBackofficeApiIT.Containers.class)
class AdminBackofficeApiIT {

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

    @Autowired
    ApplicationEvents events;

    private AdminBrowser admin;

    @BeforeEach
    void donnees() throws Exception {
        jdbc.sql("DELETE FROM admin_account").update();
        adminBootstrap.run(null);
        jdbc.sql("DELETE FROM news_item").update();
        jdbc.sql("DELETE FROM event").update();
        jdbc.sql("DELETE FROM program").update();
        jdbc.sql("DELETE FROM establishment").update();
        admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
    }

    @Test
    void actualitePublieeEpingleeUneSeuleALaFoisPuisSupprimee() throws Exception {
        String first = id(admin.post("/news", article("Première actualité", true)));
        assertThat(mvc.get().uri("/api/v1/public/news/premiere-actualite")).hasStatusOk()
                .bodyJson().extractingPath("$.featured").isEqualTo(true);

        // Épingler une autre actualité désépingle la première (un seul « À la une »).
        String second = id(admin.post("/news", article("Deuxième actualité", true)));
        assertThat(admin.get("/news/" + first)).bodyJson().extractingPath("$.featured").isEqualTo(false);
        assertThat(admin.get("/news/" + second)).bodyJson().extractingPath("$.featured").isEqualTo(true);
        assertThat(mvc.get().uri("/api/v1/public/news")).bodyJson().extractingPath("$.content[0].slug").isEqualTo("deuxieme-actualite");

        // Brouillon : invisible du public ; le portail est prévenu.
        assertThat(admin.put("/news/" + second, """
                {"title": "Deuxième actualité", "bodyHtml": "<p>Texte</p>", "category": "communiques", "published": false,
                 "featured": false}""")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/public/news/deuxieme-actualite")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(events.stream(ContentChanged.class)).anyMatch(e -> e.tags().contains("news:deuxieme-actualite"));

        assertThat(admin.delete("/news/" + first)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/public/news/premiere-actualite")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void rubriquesRenommeesAjouteesEtProtegees() throws Exception {
        String created = id(admin.post("/news/categories", """
                {"label": "Formations des experts", "labelEn": "Expert training"}"""));
        assertThat(admin.get("/news/categories")).bodyJson().extractingPath("$[*].slug").asArray().contains("formations-des-experts");
        assertThat(admin.delete("/news/categories/" + created)).hasStatus(HttpStatus.NO_CONTENT);

        // Les rubriques du menu (Communiqués, Coopération & partenariats) ne se suppriment pas.
        String communiques = jdbc.sql("SELECT CAST(id AS text) FROM news_category WHERE slug = 'communiques'").query(String.class).single();
        assertThat(admin.delete("/news/categories/" + communiques)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MENU_CATEGORY");
        assertThat(admin.put("/news/categories/" + communiques, """
                {"label": "Communiqués de presse", "labelEn": "Press releases"}""")).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/public/news/categories")).bodyJson().extractingPath("$[*].label").asArray()
                .contains("Communiqués de presse");
    }

    @Test
    void evenementAvecDatesControleesEtVisibleDuPublic() throws Exception {
        assertThat(admin.post("/events", """
                {"title": "Atelier", "bodyHtml": "<p>Programme</p>", "location": "Dakar",
                 "startsAt": "2030-03-10T09:00:00Z", "endsAt": "2030-03-09T09:00:00Z", "published": true}"""))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("END_BEFORE_START");
        String id = id(admin.post("/events", """
                {"title": "Atelier d'auto-évaluation", "bodyHtml": "<p>Programme</p>", "location": "Thiès",
                 "startsAt": "2030-03-10T09:00:00Z", "endsAt": "2030-03-11T17:00:00Z", "published": true}"""));
        assertThat(mvc.get().uri("/api/v1/public/events")).bodyJson().extractingPath("$.content[0].location").isEqualTo("Thiès");
        assertThat(admin.delete("/events/" + id)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void etablissementEtFormationAvecAvisPublies() throws Exception {
        assertThat(admin.post("/registry/establishments", """
                {"name": "Université de test", "acronym": "UT", "kind": "PUBLIC", "avis": "FAVORABLE"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("AVIS_INCOMPLETE");
        String establishment = id(admin.post("/registry/establishments", """
                {"name": "Université de test", "acronym": "UT", "kind": "PUBLIC", "city": "Dakar",
                 "avis": "FAVORABLE", "avisYear": 2025}"""));
        String program = id(admin.post("/registry/programs", """
                {"title": "Master Informatique", "level": "MASTER", "domain": "Sciences et technologies",
                 "establishmentId": "%s", "avis": "DEFAVORABLE", "avisYear": 2026}""".formatted(establishment)));

        assertThat(mvc.get().uri("/api/v1/public/establishments/ut-universite-de-test")).hasStatusOk()
                .bodyJson().extractingPath("$.habilitationAvis").isEqualTo("FAVORABLE");
        assertThat(events.stream(RegistryChanged.class)).anyMatch(e -> e.tags().contains("program:ut-master-informatique"));

        // Un établissement qui a des formations ne se supprime pas.
        assertThat(admin.delete("/registry/establishments/" + establishment)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("ESTABLISHMENT_HAS_PROGRAMS");
        assertThat(admin.delete("/registry/programs/" + program)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(admin.delete("/registry/establishments/" + establishment)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void parametresDuSiteModifiesParLAdministrateurSeulement() throws Exception {
        assertThat(mvc.get().uri("/api/v1/public/settings")).hasStatusOk()
                .bodyJson().extractingPath("$.contact.email").isEqualTo("secretariat@anaqsup.sn");

        assertThat(admin.put("/settings", """
                {"contact": {"phone": "+221 33 000 00 00", "email": "contact@anaqsup.sn", "address": "Dakar"},
                 "social": [{"network": "facebook", "url": "https://www.facebook.com/ANAQSup/"}],
                 "usefulLinks": [{"label": "MESRI", "url": "https://mesrisenegal.sn"}],
                 "intros": {"Events.lead": {"fr": "Les rencontres de l'assurance qualité.", "en": ""}}}""")).hasStatusOk();
        var settings = mvc.get().uri("/api/v1/public/settings").exchange();
        assertThat(settings).bodyJson().extractingPath("$.contact.phone").isEqualTo("+221 33 000 00 00");
        assertThat(settings).bodyJson().extractingPath("$.intros['Events.lead'].fr").isEqualTo("Les rencontres de l'assurance qualité.");
        assertThat(settings).bodyJson().extractingPath("$.intros['Events.lead'].en").isNull();

        assertThat(admin.put("/settings", """
                {"contact": {"phone": "1", "email": "contact@anaqsup.sn", "address": "Dakar"}, "social": [], "usefulLinks": [],
                 "intros": {"Inconnu.lead": {"fr": "x"}}}""")).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("UNKNOWN_INTRO");

        // Le chargé de communication publie des actualités mais ne touche pas aux paramètres du site.
        assertThat(admin.post("/accounts", """
                {"email": "com@anaqsup.test", "displayName": "Communication", "roleId": "%s", "temporaryPassword": "provisoire-2"}"""
                .formatted(roleId("Chargé de communication")))).hasStatus(HttpStatus.CREATED);
        AdminBrowser com = AdminBrowser.login(mvc, "com@anaqsup.test", "provisoire-2", "definitif-2");
        assertThat(com.get("/settings")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(com.get("/registry/establishments")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(com.get("/news")).hasStatusOk();
    }

    private static String article(String title, boolean featured) {
        return """
                {"title": "%s", "summary": "Résumé", "bodyHtml": "<p>Texte de l'article</p>", "category": "vie-institutionnelle",
                 "published": true, "publishedAt": "2026-10-01T10:00:00Z", "featured": %s}""".formatted(title, featured);
    }

    private static String id(MvcTestResult result) throws Exception {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
    }

    private String roleId(String name) {
        return jdbc.sql("SELECT CAST(id AS text) FROM admin_role WHERE name = :name").param("name", name).query(String.class).single();
    }
}
