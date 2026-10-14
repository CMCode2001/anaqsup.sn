package sn.anaqsup.portail.content;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import sn.anaqsup.portail.content.LegacyContentImport.Article;

/**
 * Reprise de l'ancien site : rapports et discours publiés par erreur en actualités (décision ANAQ-Sup du
 * 14/10/2026) — document déjà présent reconnu malgré un titre écrit autrement, actualité retirée.
 */
@SpringBootTest
@Import(LegacyContentImportIT.Containers.class)
class LegacyContentImportIT {

    private static final long WORDPRESS_ID = 990_001L;

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    @Autowired
    LegacyContentImport legacy;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void donnees() {
        jdbc.sql("INSERT INTO news_category (slug, label) VALUES ('communiques', 'Communiqués') ON CONFLICT (slug) DO NOTHING").update();
        jdbc.sql("""
                INSERT INTO publication (slug, title, kind, published_on, status, author, published_at)
                VALUES ('rapport-test-2023', 'RAPPORT D’ACTIVITÉS ET DE RÉSULTATS DE L’ANNÉE 2023', 'RAPPORT_ANNUEL',
                        DATE '2024-11-25', 'PUBLIE', 'redaction', now())""").update();
    }

    @AfterEach
    void menage() {
        jdbc.sql("DELETE FROM publication WHERE slug = 'rapport-test-2023'").update();
        jdbc.sql("DELETE FROM news_item WHERE legacy_wordpress_id = :id").param("id", WORDPRESS_ID).update();
    }

    @Test
    void documentDejaPresentReconnuMalgreLaCasseLesAccentsEtLesApostrophes() {
        assertThat(legacy.hasDocumentTitled("RAPPORT_ANNUEL", "Rapport d'activites et de resultats de l'année   2023")).isTrue();
        assertThat(legacy.hasDocumentTitled("DISCOURS", "RAPPORT D’ACTIVITÉS ET DE RÉSULTATS DE L’ANNÉE 2023")).isFalse();
        assertThat(legacy.hasDocumentTitled("RAPPORT_ANNUEL", "Rapport d'activités 2022")).isFalse();
    }

    @Test
    void actualiteRepriseParErreurRetiree() {
        legacy.importArticle(new Article(WORDPRESS_ID, "rapport-2014-test", "Rapport d’activités 2014", null,
                "<p><a href=\"https://anaqsup.sn/rapport.pdf\">Rapport</a></p>", "communiques", Instant.parse("2014-08-06T10:00:00Z"), null));
        assertThat(legacy.hasArticle(WORDPRESS_ID)).isTrue();

        assertThat(legacy.removeArticle(WORDPRESS_ID)).contains("rapport-2014-test");
        assertThat(legacy.hasArticle(WORDPRESS_ID)).isFalse();
        assertThat(legacy.removeArticle(WORDPRESS_ID)).isEmpty();
    }
}
