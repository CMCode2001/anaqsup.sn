package sn.anaqsup.portail.search.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Recherche sur tout le site (décision ANAQ-Sup du 14/10/2026) : une seule liste tous types confondus, par début de
 * mot et sans accents, titre correspondant en premier, brouillons exclus, filtres, version anglaise, pagination.
 * Mot inventé (« Zorglub ») : aucun autre contenu des tests ne le contient.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(SiteSearchApiIT.Containers.class)
class SiteSearchApiIT {

    /** Plus d'un an : hors de la période « dernière année ». */
    private static final LocalDate GUIDE_DATE = LocalDate.now().minusDays(400);

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        jdbc.sql("INSERT INTO news_category (slug, label) VALUES ('vie-institutionnelle', 'Vie institutionnelle') ON CONFLICT (slug) DO NOTHING").update();
        actualite("zorglub-titre", "Rapport Zorglub sur l’évaluation", "<p>Texte.</p>", "PUBLIE", "now() - interval '2 days'");
        actualite("zorglub-texte", "Atelier de partage", "<p>Il fut question du projet Zorglub.</p>", "PUBLIE", "now() - interval '1 day'");
        actualite("zorglub-brouillon", "Zorglub en brouillon", "<p>…</p>", "BROUILLON", "NULL");
        jdbc.sql("UPDATE news_item SET title_en = 'Zorglub report on evaluation', body_html_en = '<p>Text.</p>' WHERE slug = 'zorglub-titre'").update();
        jdbc.sql("""
                INSERT INTO event (slug, title, body_html, location, starts_at, status, author, published_at)
                VALUES ('zorglub-seminaire', 'Séminaire Zorglub', '<p>…</p>', 'Thiès', now() + interval '10 days',
                        'PUBLIE', 'redaction', now() - interval '1 day')""").update();
        jdbc.sql("""
                INSERT INTO publication (slug, title, kind, published_on, status, author, published_at)
                VALUES ('zorglub-guide', 'Guide Zorglubien d’auto-évaluation', 'GUIDE', :day, 'PUBLIE', 'redaction', now())""")
                .param("day", GUIDE_DATE).update();
        jdbc.sql("""
                INSERT INTO institutional_page (slug, title, body_html, status, author, published_at)
                VALUES ('zorglub-page', 'Page Zorglub', '<p>…</p>', 'PUBLIE', 'redaction', now())""").update();
        String establishment = jdbc.sql("""
                INSERT INTO establishment (slug, name, acronym, kind, city, habilitation_avis, habilitation_avis_year)
                VALUES ('uzg-universite-zorglub', 'Université Zorglub', 'UZG', 'PUBLIC', 'Thiès', 'FAVORABLE', 2025)
                RETURNING CAST(id AS text)""").query(String.class).single();
        jdbc.sql("""
                INSERT INTO program (slug, establishment_id, title, level, accreditation_avis, accreditation_avis_year)
                VALUES ('uzg-licence-zorglub', CAST(:e AS uuid), 'Licence Zorglub', 'LICENCE', 'DEFAVORABLE', 2026)""")
                .param("e", establishment).update();
    }

    @AfterEach
    void menage() {
        jdbc.sql("DELETE FROM news_item WHERE slug LIKE 'zorglub-%'").update();
        jdbc.sql("DELETE FROM event WHERE slug LIKE 'zorglub-%'").update();
        jdbc.sql("DELETE FROM publication WHERE slug LIKE 'zorglub-%'").update();
        jdbc.sql("DELETE FROM institutional_page WHERE slug LIKE 'zorglub-%'").update();
        jdbc.sql("DELETE FROM program WHERE slug LIKE 'uzg-%'").update();
        jdbc.sql("DELETE FROM establishment WHERE slug LIKE 'uzg-%'").update();
    }

    @Test
    void uneSeuleListeTousTypesConfondusTitreCorrespondantDabord() {
        var json = assertThat(mvc.get().uri("/api/v1/public/search?q=zorgl")).bodyJson();
        // Brouillon exclu ; 6 types ; l'actualité qui ne cite Zorglub que dans son texte arrive en dernier.
        json.extractingPath("$.page.totalElements").isEqualTo(7);
        json.extractingPath("$.content[*].type").asArray()
                .containsExactlyInAnyOrder("news", "news", "events", "publications", "establishments", "programs", "pages");
        json.extractingPath("$.content[6].slug").isEqualTo("zorglub-texte");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorgl&page=2&size=3")).bodyJson()
                .isLenientlyEqualTo("""
                        {"content": [{"slug": "zorglub-texte"}], "page": {"page": 2, "totalElements": 7, "totalPages": 3}}""");
    }

    @Test
    void precisionsDeChaqueType() {
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=news&size=1")).bodyJson()
                .isLenientlyEqualTo("""
                        {"content": [{"label": "Vie institutionnelle", "code": "vie-institutionnelle"}]}""");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=events")).bodyJson()
                .extractingPath("$.content[0].label").isEqualTo("Thiès");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=publications")).bodyJson()
                .extractingPath("$.content[0].code").isEqualTo("GUIDE");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=establishments")).bodyJson()
                .isLenientlyEqualTo("""
                        {"content": [{"type": "establishments", "label": "UZG · Thiès", "code": "FAVORABLE"}]}""");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=programs")).bodyJson()
                .isLenientlyEqualTo("""
                        {"content": [{"label": "UZG", "code": "DEFAVORABLE"}]}""");
    }

    @Test
    void filtresQuiEcartentLesTypesSansObjet() {
        // Dernière année (événement à venir compris) : ni registre, ni pages, ni le guide daté de plus d'un an.
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&period=year")).bodyJson()
                .extractingPath("$.content[*].slug").asArray()
                .containsExactlyInAnyOrder("zorglub-titre", "zorglub-texte", "zorglub-seminaire");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&period=" + GUIDE_DATE.getYear())).bodyJson()
                .extractingPath("$.content[*].slug").asArray().containsExactly("zorglub-guide");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&category=vie-institutionnelle")).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(2);
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&kind=GUIDE")).bodyJson()
                .extractingPath("$.content[*].slug").asArray().containsExactly("zorglub-guide");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=pages&period=month")).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(0);
    }

    @Test
    void accentsEtPetitsMotsIgnores() {
        assertThat(mvc.get().uri("/api/v1/public/search?q=l’evaluation zorglub&type=news")).bodyJson()
                .extractingPath("$.content[*].slug").asArray().containsExactly("zorglub-titre");
        assertThat(mvc.get().uri("/api/v1/public/search?q=Évaluation ZORGLUBIEN&type=publications")).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(1);
    }

    @Test
    void unTypePageParPageEtVersionAnglaise() {
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=news&page=1&size=1")).bodyJson()
                .isLenientlyEqualTo("""
                        {"content": [{"type": "news", "slug": "zorglub-texte"}], "page": {"totalElements": 2}}""");
        assertThat(mvc.get().uri("/api/v1/public/search?q=zorglub&type=news&lang=en&size=1")).bodyJson()
                .isLenientlyEqualTo("""
                        {"content": [{"title": "Zorglub report on evaluation", "lang": "en"}]}""");
    }

    @Test
    void requetesInvalidesOuVides() {
        for (String invalid : List.of("q=z", "q=zorglub&type=inconnu", "q=zorglub&period=1850", "q=zorglub&kind=FOO",
                "q=zorglub&category=Pas%20un%20slug", "q=zorglub&page=50", "q=zorglub&size=51")) {
            assertThat(mvc.get().uri("/api/v1/public/search?" + invalid)).as(invalid).hasStatus(HttpStatus.BAD_REQUEST);
        }
        assertThat(mvc.get().uri("/api/v1/public/search?q=l' d'")).hasStatusOk()
                .bodyJson().extractingPath("$.content").asArray().isEmpty();
    }

    private void actualite(String slug, String title, String body, String status, String publishedAt) {
        jdbc.sql("""
                INSERT INTO news_item (slug, category_id, title, body_html, status, author, published_at)
                SELECT :slug, id, :title, :body, :status, 'redaction', %s
                FROM news_category WHERE slug = 'vie-institutionnelle'""".formatted(publishedAt))
                .param("slug", slug).param("title", title).param("body", body).param("status", status).update();
    }
}
