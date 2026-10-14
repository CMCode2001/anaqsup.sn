package sn.anaqsup.portail.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Le public ne voit que ce qui est publié et dont la date de publication est atteinte. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PublicContentApiIT.Containers.class)
class PublicContentApiIT {

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
    void contenusVides() {
        jdbc.sql("TRUNCATE news_item, news_category, event, publication, institutional_page CASCADE").update();
        jdbc.sql("INSERT INTO news_category (slug, label) VALUES ('vie-institutionnelle', 'Vie institutionnelle')").update();
    }

    @Test
    void seulesLesActualitesPublieesSontVisibles() {
        actualite("publiee", "PUBLIE", "now() - interval '1 hour'");
        actualite("programmee", "PUBLIE", "now() + interval '1 day'");
        actualite("brouillon", "BROUILLON", "NULL");

        assertThat(mvc.get().uri("/api/v1/public/news")).hasStatusOk()
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(mvc.get().uri("/api/v1/public/news/brouillon")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/public/news/publiee"))
                .bodyJson().extractingPath("$.category.slug").isEqualTo("vie-institutionnelle");
    }

    @Test
    void articlePrecedentEtSuivantDansLOrdreDePublication() {
        actualite("ancien", "PUBLIE", "now() - interval '3 days'");
        actualite("milieu", "PUBLIE", "now() - interval '2 days'");
        actualite("brouillon-entre-deux", "BROUILLON", "NULL");
        actualite("recent", "PUBLIE", "now() - interval '1 day'");
        actualite("programme", "PUBLIE", "now() + interval '1 day'");

        var milieu = mvc.get().uri("/api/v1/public/news/milieu").exchange();
        assertThat(milieu).bodyJson().extractingPath("$.previous.slug").isEqualTo("ancien");
        assertThat(milieu).bodyJson().extractingPath("$.next.slug").isEqualTo("recent");
        // Aux extrémités : rien avant le plus ancien ; un article programmé n'est pas encore « suivant ».
        assertThat(mvc.get().uri("/api/v1/public/news/ancien")).bodyJson().extractingPath("$.previous").isNull();
        assertThat(mvc.get().uri("/api/v1/public/news/recent")).bodyJson().extractingPath("$.next").isNull();
    }

    @Test
    void evenementsAVenirEtPassesSontSepares() {
        evenement("a-venir", "now() + interval '10 days'");
        evenement("passe", "now() - interval '10 days'");

        assertThat(mvc.get().uri("/api/v1/public/events"))
                .bodyJson().extractingPath("$.content[0].slug").isEqualTo("a-venir");
        assertThat(mvc.get().uri("/api/v1/public/events?period=past"))
                .bodyJson().extractingPath("$.content[0].slug").isEqualTo("passe");
        assertThat(mvc.get().uri("/api/v1/public/events?period=demain")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void pageInstitutionnelleEtPublicationsFiltrees() {
        jdbc.sql("""
                INSERT INTO institutional_page (slug, title, body_html, status, author, published_at)
                VALUES ('missions', 'Nos missions', '<p>…</p>', 'PUBLIE', 'redaction', now())""").update();
        jdbc.sql("""
                INSERT INTO publication (slug, title, kind, published_on, status, author, published_at)
                VALUES ('rapport-2025', 'Rapport 2025', 'RAPPORT_ANNUEL', DATE '2026-03-01', 'PUBLIE', 'redaction', now()),
                       ('guide', 'Guide', 'GUIDE', DATE '2026-01-01', 'PUBLIE', 'redaction', now())""").update();

        assertThat(mvc.get().uri("/api/v1/public/pages/missions"))
                .bodyJson().extractingPath("$.title").isEqualTo("Nos missions");
        assertThat(mvc.get().uri("/api/v1/public/publications?kind=GUIDE"))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
    }

    @Test
    void documentsRecherchesSansAccentsEtFiltresParAnnee() {
        jdbc.sql("""
                INSERT INTO publication (slug, title, summary, kind, published_on, status, author, published_at)
                VALUES ('referentiel-programmes', 'Référentiel d''évaluation des programmes', 'Critères de l''accréditation',
                        'REFERENTIEL', DATE '2026-02-01', 'PUBLIE', 'redaction', now()),
                       ('referentiel-etablissements', 'Référentiel institutionnel', NULL,
                        'REFERENTIEL', DATE '2024-05-01', 'PUBLIE', 'redaction', now()),
                       ('guide-2026', 'Guide de l''auto-évaluation', NULL, 'GUIDE', DATE '2026-06-01', 'PUBLIE', 'redaction', now()),
                       ('brouillon-2023', 'Référentiel en préparation', NULL, 'REFERENTIEL', DATE '2023-01-01', 'BROUILLON',
                        'redaction', NULL)""").update();

        // Chaque mot doit apparaître, sans tenir compte des accents ni des majuscules.
        assertThat(mvc.get().uri("/api/v1/public/publications?q=REFERENTIEL evaluation"))
                .bodyJson().extractingPath("$.content[*].slug").asArray().containsExactly("referentiel-programmes");
        assertThat(mvc.get().uri("/api/v1/public/publications?q=accreditation"))
                .bodyJson().extractingPath("$.content[*].slug").asArray().containsExactly("referentiel-programmes");
        assertThat(mvc.get().uri("/api/v1/public/publications?q=referentiel&year=2024"))
                .bodyJson().extractingPath("$.content[*].slug").asArray().containsExactly("referentiel-etablissements");
        // Un « % » ou un « _ » saisi est cherché tel quel (pas de joker).
        assertThat(mvc.get().uri("/api/v1/public/publications?q=%25"))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);

        // Années proposées dans le filtre : documents publiés seulement, la plus récente d'abord.
        assertThat(mvc.get().uri("/api/v1/public/publications/years"))
                .bodyJson().extractingPath("$").asArray().containsExactly(2026, 2024);
        assertThat(mvc.get().uri("/api/v1/public/publications/years?kind=GUIDE"))
                .bodyJson().extractingPath("$").asArray().containsExactly(2026);
        assertThat(mvc.get().uri("/api/v1/public/publications?year=12")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void versionAnglaiseServieSiElleExisteSinonRepliSurLeFrancais() {
        actualite("traduite", "PUBLIE", "now() - interval '2 hours'");
        actualite("non-traduite", "PUBLIE", "now() - interval '1 hour'");
        jdbc.sql("""
                UPDATE news_item SET title_en = 'Translated', summary_en = 'Summary', body_html_en = '<p>EN</p>'
                WHERE slug = 'traduite'""").update();
        jdbc.sql("UPDATE news_category SET label_en = 'Institutional life'").update();

        // Liste anglaise : chaque article dans sa meilleure langue disponible, catégorie traduite.
        assertThat(mvc.get().uri("/api/v1/public/news?lang=en"))
                .bodyJson().extractingPath("$.content[*].lang").asArray().containsExactly("fr", "en");
        assertThat(mvc.get().uri("/api/v1/public/news?lang=en"))
                .bodyJson().extractingPath("$.content[*].title").asArray().containsExactly("non-traduite", "Translated");
        assertThat(mvc.get().uri("/api/v1/public/news?lang=en"))
                .bodyJson().extractingPath("$.content[1].category.label").isEqualTo("Institutional life");
        assertThat(mvc.get().uri("/api/v1/public/news/traduite?lang=en"))
                .bodyJson().extractingPath("$.bodyHtml").isEqualTo("<p>EN</p>");
        // Par défaut : français, même si une version anglaise existe.
        assertThat(mvc.get().uri("/api/v1/public/news/traduite"))
                .bodyJson().extractingPath("$.lang").isEqualTo("fr");
        assertThat(mvc.get().uri("/api/v1/public/news?lang=wo")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void articleEpingleALaUneEnPremierPuisLesPlusRecents() {
        actualite("ancienne-epinglee", "PUBLIE", "now() - interval '10 days'");
        actualite("recente", "PUBLIE", "now() - interval '1 hour'");
        jdbc.sql("UPDATE news_item SET featured = true, cover_image_key = '/demo/une.svg' WHERE slug = 'ancienne-epinglee'")
                .update();

        assertThat(mvc.get().uri("/api/v1/public/news"))
                .bodyJson().extractingPath("$.content[*].slug").asArray().containsExactly("ancienne-epinglee", "recente");
        assertThat(mvc.get().uri("/api/v1/public/news"))
                .bodyJson().extractingPath("$.content[0].coverImageUrl").isEqualTo("/demo/une.svg");
        // Un seul article à la une à la fois.
        assertThatThrownBy(() -> jdbc.sql("UPDATE news_item SET featured = true WHERE slug = 'recente'").update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("news_item_single_featured_idx");
    }

    @Test
    void versionAnglaiseIncompleteEstRefusee() {
        actualite("a-traduire", "PUBLIE", "now()");
        assertThatThrownBy(() -> jdbc.sql("UPDATE news_item SET title_en = 'Title only' WHERE slug = 'a-traduire'").update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("news_item_en_complete_chk");
    }

    @Test
    void publierSansDateDePublicationEstRefuse() {
        assertThatThrownBy(() -> jdbc.sql("""
                INSERT INTO event (slug, title, body_html, starts_at, status, author, published_at)
                VALUES ('sans-date', 'Sans date', '<p>…</p>', now(), 'PUBLIE', 'agent', NULL)""").update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("event_published_has_date_chk");
    }

    private void actualite(String slug, String status, String publishedAt) {
        jdbc.sql("""
                INSERT INTO news_item (slug, category_id, title, body_html, status, author, published_at)
                SELECT :slug, id, :slug, '<p>…</p>', :status, 'redaction', %s
                FROM news_category WHERE slug = 'vie-institutionnelle'""".formatted(publishedAt))
                .param("slug", slug).param("status", status).update();
    }

    private void evenement(String slug, String startsAt) {
        jdbc.sql("""
                INSERT INTO event (slug, title, body_html, starts_at, status, author, published_at)
                VALUES (:slug, :slug, '<p>…</p>', %s, 'PUBLIE', 'redaction', now() - interval '1 day')"""
                        .formatted(startsAt))
                .param("slug", slug).update();
    }
}
