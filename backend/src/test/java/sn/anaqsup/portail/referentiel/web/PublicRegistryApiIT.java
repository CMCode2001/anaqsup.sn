package sn.anaqsup.portail.referentiel.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Listes des habilitations et accréditations : recherche, filtres par avis, fiches, cache HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PublicRegistryApiIT.Containers.class)
class PublicRegistryApiIT {

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
    void listesVides() {
        jdbc.sql("TRUNCATE program, establishment_alias, establishment CASCADE").update();
    }

    @Test
    void ficheInconnueRenvoieUnProblemDetail404() {
        assertThat(mvc.get().uri("/api/v1/public/programs/formation-inconnue"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.code").isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void parametresInvalidesRejetes() {
        assertThat(mvc.get().uri("/api/v1/public/programs/Pas_Un_Slug")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/api/v1/public/programs?size=500")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.get().uri("/api/v1/public/establishments?avis=PEUT_ETRE")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void ficheFormationAvecAvisEtCacheHttp() {
        UUID etablissement = etablissement("universite-test", "Université Test", "UT", "FAVORABLE", 2024);
        formation("ut-master-informatique", etablissement, "Master Informatique", "FAVORABLE", 2023);

        var response = mvc.get().uri("/api/v1/public/programs/ut-master-informatique");

        assertThat(response).hasStatusOk()
                .headers().containsHeader("ETag").hasValue("Cache-Control", "max-age=300, public");
        assertThat(response).bodyJson().extractingPath("$.accreditationAvis").isEqualTo("FAVORABLE");
        assertThat(response).bodyJson().extractingPath("$.accreditationAvisYear").isEqualTo(2023);
        assertThat(response).bodyJson().extractingPath("$.establishment.slug").isEqualTo("universite-test");
    }

    @Test
    void rechercheInsensibleAuxAccentsEtSigleExactEnTete() {
        etablissement("universite-demo-de-thies", "Université Démo de Thiès", "UDT", null, null);
        etablissement("institut-thies-numerique", "Institut du Numérique de Thiès", "INT", null, null);

        assertThat(mvc.get().uri("/api/v1/public/establishments?q=universite demo thies"))
                .bodyJson().extractingPath("$.content[0].slug").isEqualTo("universite-demo-de-thies");
        assertThat(mvc.get().uri("/api/v1/public/establishments?q=int"))
                .bodyJson().extractingPath("$.content[0].slug").isEqualTo("institut-thies-numerique");
        assertThat(mvc.get().uri("/api/v1/public/establishments?q=thies"))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(2);
    }

    @Test
    void filtresParAvisEtParEtablissement() {
        UUID favorable = etablissement("favorable", "École Favorable", "EF", "FAVORABLE", 2025);
        UUID defavorable = etablissement("defavorable", "École Défavorable", "ED", "DEFAVORABLE", 2025);
        formation("ef-licence", favorable, "Licence Droit", "FAVORABLE", 2025);
        formation("ed-licence", defavorable, "Licence Gestion", "DEFAVORABLE", 2025);

        assertThat(mvc.get().uri("/api/v1/public/establishments?avis=FAVORABLE"))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        assertThat(mvc.get().uri("/api/v1/public/programs?establishment=defavorable"))
                .bodyJson().extractingPath("$.content[0].accreditationAvis").isEqualTo("DEFAVORABLE");
        assertThat(mvc.get().uri("/api/v1/public/establishments/favorable"))
                .bodyJson().extractingPath("$.programCount").isEqualTo(1);
        assertThat(mvc.get().uri("/api/v1/public/statistics"))
                .bodyJson().extractingPath("$.accreditedPrograms").isEqualTo(1);
    }

    @Test
    void lAdministrationExigeUneAuthentification() {
        assertThat(mvc.get().uri("/api/v1/admin/news")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private UUID etablissement(String slug, String name, String acronym, String avis, Integer year) {
        return jdbc.sql("""
                        INSERT INTO establishment (slug, name, acronym, kind, city, habilitation_avis, habilitation_avis_year)
                        VALUES (:slug, :name, :acronym, 'PUBLIC', 'Thiès', :avis, :year) RETURNING id""")
                .param("slug", slug).param("name", name).param("acronym", acronym)
                .param("avis", avis).param("year", year)
                .query(UUID.class).single();
    }

    private void formation(String slug, UUID establishment, String title, String avis, Integer year) {
        jdbc.sql("""
                        INSERT INTO program (slug, establishment_id, title, level, accreditation_avis, accreditation_avis_year)
                        VALUES (:slug, :e, :title, 'LICENCE', :avis, :year)""")
                .param("slug", slug).param("e", establishment).param("title", title)
                .param("avis", avis).param("year", year).update();
    }
}
