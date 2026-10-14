package sn.anaqsup.portail.comptes.web;

import static org.assertj.core.api.Assertions.assertThat;


import jakarta.servlet.http.Cookie;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.testcontainers.postgresql.PostgreSQLContainer;

import sn.anaqsup.portail.support.AdminBrowser;

/**
 * Comptes du back-office : connexion, mot de passe provisoire, rôles et règles de protection.
 * Chaque requête passe par le vrai mécanisme du navigateur (cookie de session, cookie XSRF-TOKEN renvoyé
 * dans X-XSRF-TOKEN) : pas de raccourci {@code csrf()} de spring-security-test, qui remplacerait le
 * dépôt de jetons pour tout le contexte.
 */
@SpringBootTest(properties = {
        "anaqsup.admin.bootstrap.email=admin@anaqsup.test",
        "anaqsup.admin.bootstrap.password=provisoire-1",
        "anaqsup.admin.bootstrap.name=Administratrice test",
})
@AutoConfigureMockMvc
@Import(AdminApiIT.Containers.class)
class AdminApiIT {

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

    @Autowired
    ApplicationRunner adminBootstrap;

    @BeforeEach
    void premierAdministrateurSeulement() throws Exception {
        jdbc.sql("DELETE FROM admin_account").update();
        jdbc.sql("DELETE FROM admin_role WHERE NOT system AND name <> 'Chargé de communication'").update();
        adminBootstrap.run(null);
    }

    @Test
    void premiereConnexionImposeLeChangementDuMotDePasseProvisoire() {
        AdminBrowser admin = login("admin@anaqsup.test", "provisoire-1");

        assertThat(admin.get("/auth/me")).bodyJson().extractingPath("$.mustChangePassword").isEqualTo(true);
        assertThat(admin.get("/accounts")).hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.code").isEqualTo("PASSWORD_CHANGE_REQUIRED");

        changePassword(admin, "provisoire-1", "definitif-1");

        assertThat(admin.get("/accounts")).hasStatusOk()
                .bodyJson().extractingPath("$[0].email").isEqualTo("admin@anaqsup.test");
    }

    @Test
    void jetonCsrfObligatoireEtRenouveleALaConnexion() {
        AdminBrowser browser = new AdminBrowser(mvc);
        Cookie beforeLogin = browser.xsrf;
        assertThat(beforeLogin).isNotNull();

        // Sans jeton : refusé.
        assertThat(mvc.post().uri(AdminBrowser.API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(credentials("admin@anaqsup.test", "provisoire-1")))
                .hasStatus(HttpStatus.FORBIDDEN).bodyJson().extractingPath("$.code").isEqualTo("CSRF");

        assertThat(browser.post("/auth/login", credentials("admin@anaqsup.test", "provisoire-1"))).hasStatusOk();
        assertThat(browser.xsrf).isNotNull();
        assertThat(browser.xsrf.getValue()).isNotEqualTo(beforeLogin.getValue());

        // Le jeton posé par la réponse de connexion sert directement à la requête suivante.
        changePassword(browser, "provisoire-1", "definitif-1");
    }

    @Test
    void mauvaisIdentifiantsRefusesSansDireSiLeCompteExiste() {
        AdminBrowser browser = new AdminBrowser(mvc);
        assertThat(browser.post("/auth/login", credentials("admin@anaqsup.test", "faux-mot-de-passe")))
                .hasStatus(HttpStatus.UNAUTHORIZED).bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
        assertThat(browser.post("/auth/login", credentials("inconnu@anaqsup.test", "faux-mot-de-passe")))
                .hasStatus(HttpStatus.UNAUTHORIZED).bodyJson().extractingPath("$.code").isEqualTo("BAD_CREDENTIALS");
        assertThat(browser.get("/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void chargeDeCommunicationNeGerePasLesComptes() {
        AdminBrowser admin = readyAdmin();
        createAccount(admin, "com@anaqsup.test", roleId("Chargé de communication"));

        AdminBrowser com = login("com@anaqsup.test", "provisoire-2");
        changePassword(com, "provisoire-2", "definitif-2");

        assertThat(com.get("/auth/me")).bodyJson().extractingPath("$.permissions").asArray()
                .containsExactlyInAnyOrder("ACTUALITES", "EVENEMENTS", "DOCUMENTS", "PAGES");
        assertThat(com.get("/accounts")).hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
    }

    @Test
    void compteDesactiveEstDeconnecteImmediatement() {
        AdminBrowser admin = readyAdmin();
        String comId = createAccount(admin, "com@anaqsup.test", roleId("Chargé de communication"));
        AdminBrowser com = login("com@anaqsup.test", "provisoire-2");

        assertThat(admin.put("/accounts/" + comId, """
                {"displayName": "Communication", "roleId": "%s", "enabled": false}""".formatted(roleId("Chargé de communication"))))
                .hasStatusOk();
        assertThat(com.get("/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sessionEnregistreeEnBasePourTroisHeuresEtSupprimeeALaDeconnexion() {
        jdbc.sql("DELETE FROM spring_session").update();
        AdminBrowser admin = readyAdmin();

        // En base (un redémarrage du serveur ne déconnecte personne), 3 heures d'inactivité au plus.
        assertThat(jdbc.sql("SELECT max_inactive_interval FROM spring_session WHERE principal_name = 'admin@anaqsup.test'")
                .query(Integer.class).list()).containsExactly(3 * 60 * 60);
        // Le hachage du mot de passe n'est jamais enregistré avec la session.
        assertThat(jdbc.sql("SELECT count(*) FROM spring_session_attributes WHERE position(convert_to('{bcrypt}', 'UTF8') IN attribute_bytes) > 0")
                .query(Integer.class).single()).isZero();

        assertThat(admin.post("/auth/logout", null)).hasStatus2xxSuccessful();
        assertThat(jdbc.sql("SELECT count(*) FROM spring_session").query(Integer.class).single()).isZero();
        assertThat(admin.get("/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void ilResteToujoursUnAdministrateurEtOnNeSeRetrogradePas() {
        AdminBrowser admin = readyAdmin();
        String adminId = jdbc.sql("SELECT CAST(id AS text) FROM admin_account").query(String.class).single();

        assertThat(admin.put("/accounts/" + adminId, """
                {"displayName": "Moi", "roleId": "%s", "enabled": true}""".formatted(roleId("Chargé de communication"))))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.code").isEqualTo("SELF_PROTECTED");
        assertThat(admin.send(HttpMethod.DELETE, "/accounts/" + adminId, null))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.code").isEqualTo("SELF_PROTECTED");
    }

    @Test
    void lAdministrateurCreeDesRolesMaisNeModifiePasLeSien() {
        AdminBrowser admin = readyAdmin();

        assertThat(admin.post("/roles", """
                {"name": "Évaluation", "permissions": ["AVIS"]}"""))
                .hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.permissions").asArray().containsExactly("AVIS");
        assertThat(admin.put("/roles/" + roleId("Administrateur"), """
                {"name": "Administrateur", "permissions": ["AVIS"]}"""))
                .hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.code").isEqualTo("SYSTEM_ROLE");
        assertThat(admin.post("/accounts", """
                {"email": "court@anaqsup.test", "displayName": "Court", "roleId": "%s", "temporaryPassword": "1234567"}"""
                .formatted(roleId("Chargé de communication"))))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("PASSWORD_TOO_SHORT");
    }

    // ---------------------------------------------------------------------------------------- outils

    private static String credentials(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
    }

    private AdminBrowser login(String email, String password) {
        return AdminBrowser.login(mvc, email, password, null);
    }

    private void changePassword(AdminBrowser browser, String current, String next) {
        assertThat(browser.post("/auth/password", """
                {"currentPassword": "%s", "newPassword": "%s"}""".formatted(current, next)))
                .hasStatus(HttpStatus.NO_CONTENT);
    }

    private AdminBrowser readyAdmin() {
        AdminBrowser admin = login("admin@anaqsup.test", "provisoire-1");
        changePassword(admin, "provisoire-1", "definitif-1");
        return admin;
    }

    private String createAccount(AdminBrowser admin, String email, String roleId) {
        assertThat(admin.post("/accounts", """
                {"email": "%s", "displayName": "Communication", "roleId": "%s", "temporaryPassword": "provisoire-2"}"""
                .formatted(email, roleId)))
                .hasStatus(HttpStatus.CREATED);
        return jdbc.sql("SELECT CAST(id AS text) FROM admin_account WHERE email = :email").param("email", email).query(String.class).single();
    }

    private String roleId(String name) {
        return jdbc.sql("SELECT CAST(id AS text) FROM admin_role WHERE name = :name").param("name", name).query(String.class).single();
    }
}
