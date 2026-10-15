package sn.anaqsup.portail.referentiel.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Reprise des habilitations et accréditations de l'ancien site (docs/migration/registre/registre-import.sql) :
 * le script entre dans le schéma du portail, se rejoue sans doublon, et le public ne voit que ce qui est validé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RegistryImportIT.Containers.class)
class RegistryImportIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    private static final Path SCRIPT = Path.of("..", "docs", "migration", "registre", "registre-import.sql");

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MockMvcTester mvc;

    @BeforeEach
    void baseVide() {
        jdbc.sql("TRUNCATE import_line, decision, report, program, establishment_alias, establishment CASCADE").update();
    }

    private void jouerLeScript() throws Exception {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(Files.readAllBytes(SCRIPT)));
        }
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    @Test
    void laRepriseEntreDansLeSchemaSeRejoueSansDoublonEtSeLitParLeSite() throws Exception {
        jouerLeScript();
        long etablissements = count("SELECT count(*) FROM establishment");
        long formations = count("SELECT count(*) FROM program");
        long decisions = count("SELECT count(*) FROM decision");
        long rapports = count("SELECT count(*) FROM report");
        assertThat(etablissements).isGreaterThan(100);
        assertThat(formations).isGreaterThan(500);
        assertThat(decisions).isGreaterThan(800);

        // Rejouer le script ne crée rien de plus.
        jouerLeScript();
        assertThat(count("SELECT count(*) FROM establishment")).isEqualTo(etablissements);
        assertThat(count("SELECT count(*) FROM program")).isEqualTo(formations);
        assertThat(count("SELECT count(*) FROM decision")).isEqualTo(decisions);
        assertThat(count("SELECT count(*) FROM report")).isEqualTo(rapports);

        // Toutes les décisions reprises sont validées ; les lignes douteuses restent à vérifier et ne sont pas des décisions.
        assertThat(count("SELECT count(*) FROM decision WHERE status <> 'VALIDE' OR source_ref IS NULL")).isZero();
        assertThat(count("SELECT count(*) FROM import_line")).isEqualTo(1281);
        assertThat(count("SELECT count(*) FROM import_line WHERE status = 'A_VERIFIER'")).isEqualTo(380);
        assertThat(count("SELECT count(*) FROM import_line WHERE status = 'A_VERIFIER' AND decision_id IS NOT NULL")).isZero();
        assertThat(count("SELECT count(*) FROM import_line WHERE status = 'IMPORTEE' AND decision_id IS NULL")).isZero();

        // Les avis affichés sur les fiches viennent des décisions (déclencheurs) : aucune fiche n'a d'avis sans décision.
        assertThat(count("SELECT count(*) FROM program p WHERE p.accreditation_avis IS NOT NULL AND NOT EXISTS "
                + "(SELECT 1 FROM decision_program dp JOIN decision d ON d.id = dp.decision_id WHERE dp.program_id = p.id)")).isZero();
        assertThat(count("SELECT count(*) FROM program WHERE accreditation_avis IS NOT NULL")).isGreaterThan(400);
        assertThat(count("SELECT count(*) FROM establishment WHERE habilitation_avis IS NOT NULL")).isGreaterThan(50);

        // Les écoles doctorales sont habilitées sur 6 ans : la durée du rapport est reprise, pas celle par défaut.
        assertThat(count("SELECT count(*) FROM decision WHERE kind = 'HABILITATION' AND validity_years = 6")).isPositive();

        // Côté public : un établissement qui a des composantes montre aussi les formations de celles-ci.
        String slug = jdbc.sql("""
                        SELECT e.slug FROM establishment e JOIN establishment c ON c.parent_id = e.id
                        JOIN program p ON p.establishment_id = c.id GROUP BY e.slug ORDER BY count(*) DESC LIMIT 1""")
                .query(String.class).single();
        long attendu = count("""
                SELECT count(*) FROM program WHERE establishment_id IN
                  (WITH RECURSIVE d AS (SELECT id FROM establishment WHERE slug = '%s'
                                        UNION ALL SELECT c.id FROM establishment c JOIN d ON c.parent_id = d.id)
                   SELECT id FROM d)""".formatted(slug));
        var fiche = mvc.get().uri("/api/v1/public/establishments/" + slug).exchange();
        assertThat(fiche).hasStatusOk().bodyJson().extractingPath("$.programCount").isEqualTo((int) attendu);
        assertThat(attendu).isGreaterThan(0);
        assertThat(mvc.get().uri("/api/v1/public/establishments/" + slug + "/programs?size=100").exchange())
                .hasStatus(HttpStatus.OK);

        // Un rapport public est exposé avec son lien ; l'historique d'une formation accréditée est lisible.
        String formation = jdbc.sql("""
                        SELECT p.slug FROM program p JOIN decision_program dp ON dp.program_id = p.id
                        JOIN decision d ON d.id = dp.decision_id AND d.report_id IS NOT NULL
                        JOIN report r ON r.id = d.report_id AND r.visibility = 'PUBLIC'
                        WHERE (SELECT count(*) FROM decision_program x WHERE x.program_id = p.id) = 1 LIMIT 1""")
                .query(String.class).single();
        var fiche2 = mvc.get().uri("/api/v1/public/programs/" + formation).exchange();
        assertThat(fiche2).hasStatusOk().bodyJson().extractingPath("$.decisions[0].report.url").asString()
                .startsWith("https://");
    }
}
