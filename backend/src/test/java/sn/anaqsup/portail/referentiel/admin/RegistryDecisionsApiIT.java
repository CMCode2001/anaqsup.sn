package sn.anaqsup.portail.referentiel.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import sn.anaqsup.portail.support.AdminBrowser;

/**
 * Habilitations et accréditations : composantes, décisions (brouillon, validation, archivage), historique, rapports,
 * avis mixtes, saisie rapide de la fiche. Le public ne voit que le validé ; les avis des fiches suivent la décision
 * validée la plus récente.
 */
@SpringBootTest(properties = {
        "anaqsup.admin.bootstrap.email=admin@anaqsup.test",
        "anaqsup.admin.bootstrap.password=provisoire-1",
})
@AutoConfigureMockMvc
@Import(RegistryDecisionsApiIT.Containers.class)
class RegistryDecisionsApiIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class Containers {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }
    }

    private static final String PUBLIC = "/api/v1/public";
    private static final String REPORT_URL = "https://anaqsup.sn/wp-content/uploads/2025/10/rapport-droit-public.pdf";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ApplicationRunner adminBootstrap;

    AdminBrowser admin;

    @BeforeEach
    void donnees() throws Exception {
        jdbc.sql("TRUNCATE import_line, decision, report, program, establishment_alias, establishment CASCADE").update();
        jdbc.sql("DELETE FROM admin_account").update();
        adminBootstrap.run(null);
        admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
    }

    // ------------------------------------------------------------------------------------------ outils

    private static String body(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private static String read(MvcTestResult result, String path) {
        return JsonPath.read(body(result), path).toString();
    }

    private String establishment(String name, String kind, String nature, String parentId) {
        var created = admin.post("/registry/establishments", """
                {"name": "%s", "kind": "%s", "nature": "%s", "parentId": %s}""".formatted(name, kind, nature,
                parentId == null ? "null" : "\"" + parentId + "\""));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return read(created, "$.id");
    }

    private String program(String establishmentId, String title, String level) {
        var created = admin.post("/registry/programs", """
                {"title": "%s", "level": "%s", "establishmentId": "%s"}""".formatted(title, level, establishmentId));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return read(created, "$.slug");
    }

    private String programId(String slug) {
        return jdbc.sql("SELECT id FROM program WHERE slug = :slug").param("slug", slug).query(String.class).single();
    }

    private String slugOf(String establishmentId) {
        return jdbc.sql("SELECT slug FROM establishment WHERE id = CAST(:id AS uuid)").param("id", establishmentId)
                .query(String.class).single();
    }

    private static String accreditation(String establishmentId, String date, int years, String programs, String report) {
        return """
                {"kind": "ACCREDITATION", "establishmentId": "%s", %s "validityYears": %d, %s "programs": %s}"""
                .formatted(establishmentId, date.length() == 4 ? "\"year\": " + date + "," : "\"decidedOn\": \"" + date + "\",",
                        years, report == null ? "" : "\"report\": " + report + ",", programs);
    }

    private static String report(String title, String signedOn, String visibility) {
        return """
                {"title": "%s", "url": "%s", "signedOn": "%s", "visibility": "%s"}"""
                .formatted(title, REPORT_URL, signedOn, visibility);
    }

    private static String avis(String programId, String avis) {
        return "{\"programId\": \"" + programId + "\", \"avis\": \"" + avis + "\"}";
    }

    private String createValidated(String json) {
        var created = admin.post("/registry/decisions", json);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String id = read(created, "$.id");
        assertThat(admin.post("/registry/decisions/" + id + "/validate", null)).hasStatusOk();
        return id;
    }

    private long decisions() {
        return jdbc.sql("SELECT count(*) FROM decision").query(Long.class).single();
    }

    // ------------------------------------------------------------------- composantes et formations

    @Test
    void lesFormationsDUnEtablissementIncluentCellesDeSesComposantes() {
        String ugb = establishment("Université Gaston Berger", "PUBLIC", "UNIVERSITE", null);
        String ufr = establishment("UFR Sciences juridiques et politiques", "PUBLIC", "UFR_FACULTE", ugb);
        String ed = establishment("École doctorale des sciences et technologies", "PUBLIC", "ECOLE_DOCTORALE", ugb);
        String droit = program(ufr, "Licence en Droit Public", "LICENCE");
        String doctorat = program(ed, "Doctorat en Informatique", "DOCTORAT");
        String directe = program(ugb, "Master en Gouvernance", "MASTER");
        String autre = program(establishment("Université Alioune Diop", "PUBLIC", "UNIVERSITE", null), "Licence en Lettres", "LICENCE");

        String slug = slugOf(ugb);
        var fiche = mvc.get().uri(PUBLIC + "/establishments/" + slug).exchange();
        assertThat(fiche).hasStatusOk();
        assertThat(fiche).bodyJson().extractingPath("$.programCount").isEqualTo(3);
        assertThat(fiche).bodyJson().extractingPath("$.components.length()").isEqualTo(2);
        assertThat(fiche).bodyJson().extractingPath("$.components[0].programCount").isEqualTo(1);

        // En cliquant sur l'établissement : toutes ses formations, y compris celles de ses composantes.
        var liste = mvc.get().uri(PUBLIC + "/establishments/" + slug + "/programs").exchange();
        assertThat(liste).hasStatusOk();
        assertThat(liste).bodyJson().extractingPath("$.page.totalElements").isEqualTo(3);
        String titres = body(liste);
        assertThat(titres).contains(droit).contains(doctorat).contains(directe).doesNotContain(autre);

        // Le filtre existant de la liste des formations prend lui aussi les composantes.
        assertThat(mvc.get().uri(PUBLIC + "/programs?establishment=" + slug).exchange())
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(3);
        // Depuis la composante, on retrouve son établissement.
        var composante = mvc.get().uri(PUBLIC + "/establishments/" + slugOf(ufr)).exchange();
        assertThat(composante).bodyJson().extractingPath("$.parent.slug").isEqualTo(slug);
        assertThat(mvc.get().uri(PUBLIC + "/establishments/inconnu/programs")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void lesReglesDesComposantes() {
        String ugb = establishment("Université Gaston Berger", "PUBLIC", "UNIVERSITE", null);
        String ufr = establishment("UFR Lettres", "PUBLIC", "UFR_FACULTE", ugb);
        // Une composante a le type de son établissement.
        assertThat(admin.post("/registry/establishments", """
                {"name": "UFR privée", "kind": "PRIVE", "parentId": "%s"}""".formatted(ugb)))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("PARENT_KIND_MISMATCH");
        // Un établissement ne devient pas la composante de sa propre composante.
        assertThat(admin.put("/registry/establishments/" + ugb, """
                {"name": "Université Gaston Berger", "kind": "PUBLIC", "parentId": "%s"}""".formatted(ufr)))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("PARENT_CYCLE");
        // Un établissement qui a des composantes ne se supprime pas ; détacher la composante le permet.
        assertThat(admin.delete("/registry/establishments/" + ugb)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("ESTABLISHMENT_HAS_COMPONENTS");
        assertThat(admin.delete("/registry/establishments/" + ufr + "/parent")).hasStatusOk()
                .bodyJson().extractingPath("$.parentId").isNull();
        assertThat(admin.delete("/registry/establishments/" + ugb)).hasStatus(HttpStatus.NO_CONTENT);
    }

    // ------------------------------------------------------------------------------ cycle de vie

    @Test
    void unBrouillonEstInvisibleJusquaLaValidationPuisArchive() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String slug = program(ufr, "Licence en Droit Public", "LICENCE");
        String json = accreditation(ufr, "2025-11-25", 5, "[" + avis(programId(slug), "FAVORABLE") + "]", null);

        var created = admin.post("/registry/decisions", json);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.status").isEqualTo("BROUILLON");
        assertThat(created).bodyJson().extractingPath("$.validUntil").isEqualTo("2030-11-24");
        String id = read(created, "$.id");

        // Brouillon : la fiche publique n'affiche encore aucun avis.
        assertThat(mvc.get().uri(PUBLIC + "/programs/" + slug).exchange()).bodyJson()
                .extractingPath("$.accreditationAvis").isNull();
        assertThat(mvc.get().uri(PUBLIC + "/programs?avis=FAVORABLE").exchange()).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(0);

        // Validation : l'avis et l'année s'affichent, l'historique et la fin de validité aussi.
        var validated = admin.post("/registry/decisions/" + id + "/validate", null);
        assertThat(validated).hasStatusOk();
        assertThat(validated).bodyJson().extractingPath("$.status").isEqualTo("VALIDE");
        assertThat(validated).bodyJson().extractingPath("$.validatedBy").isEqualTo("admin@anaqsup.test");
        var fiche = mvc.get().uri(PUBLIC + "/programs/" + slug).exchange();
        assertThat(fiche).bodyJson().extractingPath("$.accreditationAvis").isEqualTo("FAVORABLE");
        assertThat(fiche).bodyJson().extractingPath("$.accreditationAvisYear").isEqualTo(2025);
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].validUntil").isEqualTo("2030-11-24");
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].inForce").isEqualTo(true);
        assertThat(mvc.get().uri(PUBLIC + "/programs?avis=FAVORABLE").exchange()).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(1);

        // Une décision validée ne se modifie ni ne se supprime ; on ne la valide pas deux fois.
        assertThat(admin.put("/registry/decisions/" + id, json)).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("DECISION_NOT_EDITABLE");
        assertThat(admin.delete("/registry/decisions/" + id)).hasStatus(HttpStatus.CONFLICT);
        assertThat(admin.post("/registry/decisions/" + id + "/validate", null)).hasStatus(HttpStatus.CONFLICT);

        // Archivage : elle sort du public, le statut courant est recalculé sans elle.
        assertThat(admin.post("/registry/decisions/" + id + "/archive", null)).hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("ARCHIVE");
        var apres = mvc.get().uri(PUBLIC + "/programs/" + slug).exchange();
        assertThat(apres).bodyJson().extractingPath("$.accreditationAvis").isNull();
        assertThat(apres).bodyJson().extractingPath("$.decisions.length()").isEqualTo(0);
        assertThat(admin.post("/registry/decisions/" + id + "/archive", null)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void unBrouillonSeCorrigeEtSeSupprime() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String slug = program(ufr, "Licence en Droit Public", "LICENCE");
        String id = read(admin.post("/registry/decisions",
                accreditation(ufr, "2025-11-25", 5, "[" + avis(programId(slug), "DEFAVORABLE") + "]", null)), "$.id");

        var corrige = admin.put("/registry/decisions/" + id,
                accreditation(ufr, "2025-11-25", 6, "[" + avis(programId(slug), "FAVORABLE") + "]", null));
        assertThat(corrige).hasStatusOk();
        assertThat(corrige).bodyJson().extractingPath("$.validityYears").isEqualTo(6);
        assertThat(corrige).bodyJson().extractingPath("$.programs[0].avis").isEqualTo("FAVORABLE");
        assertThat(corrige).bodyJson().extractingPath("$.programs.length()").isEqualTo(1);

        assertThat(admin.delete("/registry/decisions/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(admin.get("/registry/decisions/" + id)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(admin.post("/registry/decisions/00000000-0000-0000-0000-00000000dead/validate", null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------------------------- historique

    @Test
    void l_historiqueGardeToutesLesDecisionsEtLaPlusRecenteFaitLeStatut() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String slug = program(ufr, "Licence en Droit Public", "LICENCE");
        String programs = "[%s]";
        // Refus en 2019, accord en 2025 : le programme n'apparaît qu'une fois, avec ses deux décisions.
        createValidated(accreditation(ufr, "2019-07-10", 5, programs.formatted(avis(programId(slug), "DEFAVORABLE")), null));
        createValidated(accreditation(ufr, "2025-11-25", 5, programs.formatted(avis(programId(slug), "FAVORABLE")), null));

        var fiche = mvc.get().uri(PUBLIC + "/programs/" + slug).exchange();
        assertThat(fiche).bodyJson().extractingPath("$.accreditationAvis").isEqualTo("FAVORABLE");
        assertThat(fiche).bodyJson().extractingPath("$.accreditationAvisYear").isEqualTo(2025);
        assertThat(fiche).bodyJson().extractingPath("$.decisions.length()").isEqualTo(2);
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].year").isEqualTo(2025);
        assertThat(fiche).bodyJson().extractingPath("$.decisions[1].avis").isEqualTo("DEFAVORABLE");
        assertThat(fiche).bodyJson().extractingPath("$.decisions[1].validUntil").isEqualTo("2024-07-09");
        assertThat(fiche).bodyJson().extractingPath("$.decisions[1].inForce").isEqualTo(false);
        assertThat(mvc.get().uri(PUBLIC + "/programs").exchange()).bodyJson()
                .extractingPath("$.page.totalElements").isEqualTo(1);
    }

    @Test
    void sansDateExacteDuConseilLaFinDeValiditeEstEstimee() {
        String ufr = establishment("UFR Communication", "PUBLIC", "UFR_FACULTE", null);
        String slug = program(ufr, "Licence en Communication digitale", "LICENCE");
        createValidated(accreditation(ufr, "2024", 5, "[" + avis(programId(slug), "FAVORABLE") + "]", null));
        var fiche = mvc.get().uri(PUBLIC + "/programs/" + slug).exchange();
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].decidedOn").isNull();
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].validUntilEstimated").isEqualTo(true);
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].validUntil").isEqualTo("2029-12-30");
    }

    @Test
    void habilitationDUnEtablissementEtAvisMixteDesFormations() {
        String ism = establishment("Institut supérieur de Management", "PRIVE", "INSTITUT", null);
        String slug = slugOf(ism);
        String licence = program(ism, "Licence en Gestion", "LICENCE");
        String master = program(ism, "Master en Finance", "MASTER");
        createValidated("""
                {"kind": "HABILITATION", "establishmentId": "%s", "decidedOn": "2020-09-01", "validityYears": 10,
                 "avis": "FAVORABLE"}""".formatted(ism));
        // Habilitation complémentaire : une formation acceptée, l'autre refusée → « MIXTE » pour l'établissement.
        createValidated("""
                {"kind": "HABILITATION_COMPLEMENTAIRE", "establishmentId": "%s", "decidedOn": "2023-01-15", "validityYears": 10,
                 "programs": [%s, %s]}""".formatted(ism, avis(programId(licence), "FAVORABLE"), avis(programId(master), "DEFAVORABLE")));

        var fiche = mvc.get().uri(PUBLIC + "/establishments/" + slug).exchange();
        assertThat(fiche).bodyJson().extractingPath("$.habilitationAvis").isEqualTo("FAVORABLE");
        assertThat(fiche).bodyJson().extractingPath("$.habilitationAvisYear").isEqualTo(2020);
        assertThat(fiche).bodyJson().extractingPath("$.currentHabilitation.validUntil").isEqualTo("2030-08-31");
        var historique = mvc.get().uri(PUBLIC + "/establishments/" + slug + "/decisions").exchange();
        assertThat(historique).bodyJson().extractingPath("$.length()").isEqualTo(2);
        assertThat(historique).bodyJson().extractingPath("$[0].kind").isEqualTo("HABILITATION_COMPLEMENTAIRE");
        assertThat(historique).bodyJson().extractingPath("$[0].avis").isEqualTo("MIXTE");
        assertThat(mvc.get().uri(PUBLIC + "/programs/" + slugOfProgram("Master en Finance")).exchange()).bodyJson()
                .extractingPath("$.accreditationAvis").isEqualTo("DEFAVORABLE");
    }

    private String slugOfProgram(String title) {
        return jdbc.sql("SELECT slug FROM program WHERE title = :t").param("t", title).query(String.class).single();
    }

    // ------------------------------------------------------------------------------------ rapports

    @Test
    void unRapportCollectifServLesFormationsEtUnRapportPriveNEstJamaisExpose() {
        String ed = establishment("École doctorale PCSTUI", "PUBLIC", "ECOLE_DOCTORALE", null);
        String chimie = program(ed, "Doctorat Chimie moléculaire", "DOCTORAT");
        String laser = program(ed, "Doctorat Laser et Optique", "DOCTORAT");
        String rapport = report("Rapport d'évaluation externe de l'école doctorale PCSTUI", "2025-05-02", "PUBLIC");
        createValidated(accreditation(ed, "2025-05-20", 6,
                "[" + avis(programId(chimie), "FAVORABLE") + "," + avis(programId(laser), "DEFAVORABLE") + "]", rapport));

        var fiche = mvc.get().uri(PUBLIC + "/programs/" + chimie).exchange();
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].report.url").isEqualTo(REPORT_URL);
        assertThat(fiche).bodyJson().extractingPath("$.decisions[0].report.signedOn").isEqualTo("2025-05-02");
        assertThat(mvc.get().uri(PUBLIC + "/programs/" + laser).exchange()).bodyJson()
                .extractingPath("$.accreditationAvis").isEqualTo("DEFAVORABLE");
        assertThat(jdbc.sql("SELECT count(*) FROM report").query(Long.class).single()).isEqualTo(1);   // un seul rapport pour les deux

        // Le même lien est réutilisé ; rendu privé, il disparaît du public mais reste lisible dans le back-office.
        jdbc.sql("UPDATE report SET visibility = 'PRIVE'").update();
        assertThat(mvc.get().uri(PUBLIC + "/programs/" + chimie).exchange()).bodyJson()
                .extractingPath("$.decisions[0].report").isNull();
        var interne = admin.get("/registry/decisions?status=VALIDE");
        assertThat(interne).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
        String id = read(interne, "$.content[0].id");
        assertThat(admin.get("/registry/decisions/" + id)).bodyJson().extractingPath("$.report.visibility").isEqualTo("PRIVE");
    }

    // ------------------------------------------------------------------------------- contrôles

    @Test
    void lesAnomaliesSontToutesListeesEtRienNEstEcrit() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String autre = establishment("Université Alioune Diop", "PUBLIC", "UNIVERSITE", null);
        String droit = program(ufr, "Licence en Droit Public", "LICENCE");
        String lettres = program(autre, "Licence en Lettres", "LICENCE");
        long avant = decisions();

        var refus = admin.post("/registry/decisions", """
                {"kind": "ACCREDITATION", "establishmentId": "%s", "decidedOn": "2999-01-01", "validityYears": 5,
                 "avis": "FAVORABLE", "report": {"title": "Rapport", "url": "ftp://x"},
                 "programs": [%s, %s, %s]}""".formatted(ufr, avis(programId(droit), "FAVORABLE"),
                avis(programId(droit), "DEFAVORABLE"), avis(programId(lettres), "FAVORABLE")));
        assertThat(refus).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refus).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_REFUSEE");
        String corps = body(refus);
        assertThat(corps).contains("date_dans_le_futur", "avis_en_double", "url_invalide", "programme_en_double",
                "programme_hors_etablissement");
        assertThat(decisions()).isEqualTo(avant);
        assertThat(jdbc.sql("SELECT count(*) FROM report").query(Long.class).single()).isZero();
    }

    private void refuse(String json, String regle) {
        var refus = admin.post("/registry/decisions", json);
        assertThat(refus).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body(refus)).contains(regle);
    }

    @Test
    void chaqueRegleDeValidation() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String ism = establishment("Institut supérieur de Management", "PRIVE", "INSTITUT", null);
        String droit = program(ufr, "Licence en Droit Public", "LICENCE");
        String ok = "[" + avis(programId(droit), "FAVORABLE") + "]";
        long avant = decisions();

        refuse(accreditation(ufr, "2013-12-31", 5, ok, null), "annee_avant_2014");
        refuse(accreditation(ufr, "2025-11-25", 5, ok, report("Rapport", "2026-02-15", "PUBLIC")), "date_avant_signature");
        refuse(accreditation("00000000-0000-0000-0000-00000000dead", "2025-11-25", 5, "[]", null), "inconnu");
        refuse(accreditation(ufr, "2025-11-25", 5, "[]", null), "programmes_requis");
        refuse(accreditation(ufr, "2025-11-25", 5, "[" + avis("00000000-0000-0000-0000-00000000dead", "FAVORABLE") + "]", null),
                "programme_inconnu");
        refuse(accreditation(ism, "2025-11-25", 5, ok, null), "programme_hors_etablissement");
        refuse("""
                {"kind": "HABILITATION", "establishmentId": "%s", "decidedOn": "2025-11-25", "validityYears": 10}""".formatted(ism),
                "avis_manquant");
        refuse("""
                {"kind": "HABILITATION", "establishmentId": "%s", "validityYears": 10, "avis": "FAVORABLE"}""".formatted(ism),
                "annee_requise");
        refuse("""
                {"kind": "HABILITATION", "establishmentId": "%s", "decidedOn": "2025-11-25", "year": 2024, "validityYears": 10,
                 "avis": "FAVORABLE"}""".formatted(ism), "annee_incoherente");
        refuse("""
                {"kind": "HABILITATION", "establishmentId": "%s", "decidedOn": "2025-11-25", "validityYears": 10,
                 "avis": "FAVORABLE", "previousId": "00000000-0000-0000-0000-00000000dead"}""".formatted(ism), "inconnue");
        assertThat(decisions()).isEqualTo(avant);

        // Une décision précédente doit concerner le même établissement.
        String ancienne = createValidated("""
                {"kind": "HABILITATION", "establishmentId": "%s", "decidedOn": "2015-03-01", "validityYears": 10,
                 "avis": "FAVORABLE"}""".formatted(ism));
        refuse(accreditation(ufr, "2025-11-25", 5, ok, null).replace("\"validityYears\"", "\"previousId\": \"" + ancienne
                + "\", \"validityYears\""), "autre_etablissement");

        // Les contrôles du schéma : durée et type.
        assertThat(admin.post("/registry/decisions", accreditation(ufr, "2025-11-25", 0, ok, null)))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(admin.post("/registry/decisions", accreditation(ufr, "2025-11-25", 5, ok, null)
                .replace("ACCREDITATION", "AUTRE"))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void desAvertissementsNeBloquentPas() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String droit = program(ufr, "Licence en Droit Public", "LICENCE");
        var cree = admin.post("/registry/decisions", accreditation(ufr, "2025-11-25", 5,
                "[" + avis(programId(droit), "FAVORABLE") + "]",
                "{\"title\": \"Rapport sur autre chose\", \"url\": \"https://exemple.org/autre.pdf\"}"));
        assertThat(cree).hasStatus(HttpStatus.CREATED);
        String avertissements = body(cree);
        assertThat(avertissements).contains("anaqsup.sn").contains("ne ressemble pas");
    }

    // ------------------------------------------------------------------------------ saisie rapide

    @Test
    void laSaisieRapideDeLaFicheCreeUneDecisionValidee() {
        String ism = establishment("Institut supérieur de Management", "PRIVE", "INSTITUT", null);
        long avant = decisions();
        String corps = """
                {"name": "Institut supérieur de Management", "kind": "PRIVE", "avis": "FAVORABLE", "avisYear": 2024}""";

        var maj = admin.put("/registry/establishments/" + ism, corps);
        assertThat(maj).hasStatusOk();
        assertThat(maj).bodyJson().extractingPath("$.avis").isEqualTo("FAVORABLE");
        assertThat(maj).bodyJson().extractingPath("$.avisYear").isEqualTo(2024);
        assertThat(decisions()).isEqualTo(avant + 1);
        assertThat(jdbc.sql("SELECT status || kind FROM decision").query(String.class).single()).isEqualTo("VALIDEHABILITATION");

        // Renvoyer la même fiche ne crée rien ; l'identité (type, nature) reste inchangée.
        assertThat(admin.put("/registry/establishments/" + ism, corps)).hasStatusOk();
        assertThat(decisions()).isEqualTo(avant + 1);

        // Changer l'avis crée une nouvelle décision ; antidater ou retirer l'avis est refusé.
        assertThat(admin.put("/registry/establishments/" + ism, corps.replace("FAVORABLE", "DEFAVORABLE").replace("2024", "2026")))
                .hasStatusOk().bodyJson().extractingPath("$.avis").isEqualTo("DEFAVORABLE");
        assertThat(admin.put("/registry/establishments/" + ism, corps.replace("2024", "2020")))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("NEWER_DECISION_EXISTS");
        assertThat(admin.put("/registry/establishments/" + ism, """
                {"name": "Institut supérieur de Management", "kind": "PRIVE"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("AVIS_REMOVAL_VIA_DECISIONS");
        assertThat(admin.put("/registry/establishments/" + ism, """
                {"name": "Institut supérieur de Management", "kind": "PRIVE", "avis": "FAVORABLE"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("AVIS_INCOMPLETE");

        // Un avis saisi depuis la fiche part avec elle (comme avant) ; l'historique n'est gardé que pour les vraies décisions.
        assertThat(admin.delete("/registry/establishments/" + ism)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(decisions()).isEqualTo(avant);
    }

    @Test
    void lesVraiesDecisionsProtegentLaSuppressionDeLaFiche() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        String slug = program(ufr, "Licence en Droit Public", "LICENCE");
        createValidated(accreditation(ufr, "2025-11-25", 5, "[" + avis(programId(slug), "FAVORABLE") + "]", null));
        assertThat(admin.delete("/registry/programs/" + programId(slug))).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("PROGRAM_HAS_DECISIONS");
        assertThat(admin.delete("/registry/establishments/" + ufr)).hasStatus(HttpStatus.BAD_REQUEST);
        // Même archivée, une décision reste dans l'historique : la fiche ne peut pas disparaître.
        String id = jdbc.sql("SELECT id FROM decision").query(String.class).single();
        assertThat(admin.post("/registry/decisions/" + id + "/archive", null)).hasStatusOk();
        assertThat(admin.delete("/registry/programs/" + programId(slug))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void laSaisieRapideD_uneFormationEtLaSuppressionProtegee() {
        String ufr = establishment("UFR Sciences juridiques", "PUBLIC", "UFR_FACULTE", null);
        var cree = admin.post("/registry/programs", """
                {"title": "Licence en Droit Public", "level": "LICENCE", "establishmentId": "%s", "avis": "DEFAVORABLE",
                 "avisYear": 2019, "optionParcours": "option Droit des affaires"}""".formatted(ufr));
        assertThat(cree).hasStatus(HttpStatus.CREATED);
        assertThat(cree).bodyJson().extractingPath("$.avis").isEqualTo("DEFAVORABLE");
        assertThat(cree).bodyJson().extractingPath("$.optionParcours").isEqualTo("option Droit des affaires");
        String id = read(cree, "$.id");
        assertThat(admin.put("/registry/programs/" + id, """
                {"title": "Licence en Droit Public", "level": "LICENCE", "establishmentId": "%s", "avis": "FAVORABLE",
                 "avisYear": 2025}""".formatted(ufr))).hasStatusOk().bodyJson().extractingPath("$.avis").isEqualTo("FAVORABLE");
        var historique = mvc.get().uri(PUBLIC + "/programs/" + read(cree, "$.slug")).exchange();
        assertThat(historique).bodyJson().extractingPath("$.decisions.length()").isEqualTo(2);
        // La suppression de la fiche emporte ses avis de saisie rapide.
        assertThat(admin.delete("/registry/programs/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(decisions()).isZero();
    }

    // ------------------------------------------------------------------------------------ accès

    @Test
    void lesDecisionsExigentUneConnexion() {
        var anonyme = new AdminBrowser(mvc);
        assertThat(anonyme.get("/registry/decisions")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(anonyme.post("/registry/decisions", "{}")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(anonyme.post("/registry/decisions/00000000-0000-0000-0000-000000000000/validate", null))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(admin.get("/registry/decisions")).hasStatusOk();
    }
}
