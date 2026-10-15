package sn.anaqsup.portail.content.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

import sn.anaqsup.portail.content.persistence.TranslationRepository;
import sn.anaqsup.portail.content.persistence.TranslationRepository.Type;

/**
 * Pages légales créées par la migration V15 (décision ANAQ-Sup du 14/10/2026) : publiées en français et en
 * anglais, la version anglaise étant tenue pour une traduction automatique à jour (DeepL ne la refait que si le
 * français change). V16 : plus de YouTube (chaîne supprimée), ni dans les réseaux sociaux ni dans le texte.
 * Contexte propre (propriété dédiée) : base fraîchement migrée, que les autres tests ne vident pas.
 */
@SpringBootTest(properties = "anaqsup.test.contexte=pages-legales")
@AutoConfigureMockMvc
@Import(LegalPagesIT.Containers.class)
class LegalPagesIT {

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
    TranslationRepository translations;

    @Test
    void pagesLegalesPublieesEnFrancaisEtEnAnglais() {
        assertThat(mvc.get().uri("/api/v1/public/pages/mentions-legales")).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"title": "Mentions légales", "lang": "fr"}""");
        assertThat(mvc.get().uri("/api/v1/public/pages/mentions-legales?lang=en")).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"title": "Legal notice", "lang": "en"}""");
        assertThat(mvc.get().uri("/api/v1/public/pages/donnees-personnelles")).hasStatusOk()
                .bodyJson().extractingPath("$.bodyHtml").asString()
                .contains("anaqsup_lecteur", "[À compléter", "Les icônes Facebook, X et LinkedIn,")
                // Pages publiques : rien sur l'outil interne de l'ANAQ-Sup (décision du 14/10/2026).
                .doesNotContain("espace de gestion", "ANAQSUP_SESSION", "XSRF", "mot de passe", "YouTube");
        var english = mvc.get().uri("/api/v1/public/pages/donnees-personnelles?lang=en").exchange();
        assertThat(english).bodyJson().extractingPath("$.title").isEqualTo("Personal data and cookies");
        assertThat(english).bodyJson().extractingPath("$.bodyHtml").asString()
                .doesNotContain("management area", "ANAQSUP_SESSION", "XSRF", "password", "YouTube");
    }

    @Test
    void reseauxSociauxSansYoutube() {
        assertThat(mvc.get().uri("/api/v1/public/settings")).hasStatusOk()
                .bodyJson().extractingPath("$.social[*].network").asArray().containsExactly("facebook", "x", "linkedin");
    }

    @Test
    void versionAnglaiseConsidereeCommeAJour() {
        assertThat(translations.pending(Type.PAGE, 100, 12))
                .noneMatch(page -> page.slug().equals("mentions-legales") || page.slug().equals("donnees-personnelles"));
    }
}
