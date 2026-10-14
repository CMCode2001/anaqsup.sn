package sn.anaqsup.portail.content.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.jayway.jsonpath.JsonPath;

import sn.anaqsup.portail.content.MachineTranslator;
import sn.anaqsup.portail.support.AdminBrowser;

/**
 * Ressources documentaires : PDF téléversé, vignette tirée de la première page, publication,
 * liste publique et traduction anglaise automatique (traducteur factice à la place de DeepL).
 */
@SpringBootTest(properties = {
        "anaqsup.admin.bootstrap.email=admin@anaqsup.test",
        "anaqsup.admin.bootstrap.password=provisoire-1",
})
@AutoConfigureMockMvc
@Import(AdminPublicationsApiIT.Config.class)
class AdminPublicationsApiIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18-alpine");
        }

        /** Traduction prévisible : « [EN] » devant chaque texte (dans le premier paragraphe pour le HTML). */
        @Bean
        @Primary
        MachineTranslator fakeTranslator() {
            return new MachineTranslator() {
                @Override
                public boolean available() {
                    return true;
                }

                @Override
                public List<String> toEnglish(List<String> texts, boolean html) {
                    return texts.stream().map(text -> html ? text.replaceFirst("<p>", "<p>[EN] ") : "[EN] " + text).toList();
                }
            };
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

    @BeforeEach
    void donnees() throws Exception {
        jdbc.sql("DELETE FROM admin_account").update();
        adminBootstrap.run(null);
        jdbc.sql("DELETE FROM publication").update();
    }

    @Test
    void documentTeleverseAvecVignettePublieEtTraduit() throws Exception {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
        byte[] pdf = pdf("Rapport annuel 2026");

        // Téléversement : le PDF et sa vignette (première page en JPEG).
        var upload = admin.upload("/media/documents", new MockMultipartFile("file", "Rapport annuel 2026.pdf", "application/pdf", pdf));
        assertThat(upload).hasStatus(HttpStatus.CREATED);
        String fileKey = read(upload, "$.file.key");
        String coverKey = read(upload, "$.thumbnail.key");
        assertThat(upload).bodyJson().extractingPath("$.thumbnail.contentType").isEqualTo("image/jpeg");
        assertThat(upload).bodyJson().extractingPath("$.file.sizeBytes").isEqualTo(pdf.length);

        var created = admin.post("/publications", """
                {"title": "Rapport annuel d'activité 2026", "summary": "Bilan de l'année.", "kind": "RAPPORT_ANNUEL",
                 "publishedOn": "2026-10-01", "published": true, "fileKey": "%s", "coverKey": "%s"}""".formatted(fileKey, coverKey));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.slug").isEqualTo("rapport-annuel-d-activite-2026");
        assertThat(created).bodyJson().extractingPath("$.coverGenerated").isEqualTo(true);
        String id = read(created, "$.id");

        // Site public : carte du document (PDF, taille, vignette) et fichier consultable sous son nom d'origine.
        var publications = mvc.get().uri("/api/v1/public/publications").exchange();
        assertThat(publications).bodyJson().extractingPath("$.content[0].fileType").isEqualTo("PDF");
        assertThat(publications).bodyJson().extractingPath("$.content[0].fileSizeBytes").isEqualTo(pdf.length);
        assertThat(publications).bodyJson().extractingPath("$.content[0].fileUrl").isEqualTo("/media/" + fileKey);
        assertThat(publications).bodyJson().extractingPath("$.content[0].coverImageUrl").isEqualTo("/media/" + coverKey);
        assertThat(mvc.get().uri("/api/v1/public/media/" + fileKey)).hasStatusOk()
                .hasContentType("application/pdf")
                .hasHeader("Content-Disposition",
                        "inline; filename=\"Rapport annuel 2026.pdf\"; filename*=UTF-8''Rapport%20annuel%202026.pdf");

        // Traduction automatique, juste après l'enregistrement.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(mvc.get().uri("/api/v1/public/publications?lang=en"))
                        .bodyJson().extractingPath("$.content[0].title").isEqualTo("[EN] Rapport annuel d'activité 2026"));
        assertThat(admin.get("/publications/" + id)).bodyJson().extractingPath("$.englishAuto").isEqualTo(true);

        // Le français change : la traduction automatique (renvoyée telle quelle) est refaite.
        assertThat(admin.put("/publications/" + id, """
                {"title": "Rapport d'activité 2026", "summary": "Bilan de l'année.", "kind": "RAPPORT_ANNUEL",
                 "publishedOn": "2026-10-01", "published": true, "fileKey": "%s", "coverKey": "%s",
                 "titleEn": "[EN] Rapport annuel d'activité 2026", "summaryEn": "[EN] Bilan de l'année."}"""
                .formatted(fileKey, coverKey))).hasStatusOk();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(admin.get("/publications/" + id)).bodyJson().extractingPath("$.titleEn")
                        .isEqualTo("[EN] Rapport d'activité 2026"));

        // Une traduction corrigée par l'éditeur n'est plus jamais écrasée.
        assertThat(admin.put("/publications/" + id, """
                {"title": "Rapport d'activité 2026", "kind": "RAPPORT_ANNUEL", "publishedOn": "2026-10-01",
                 "published": true, "fileKey": "%s", "coverKey": "%s", "titleEn": "2026 Activity Report"}"""
                .formatted(fileKey, coverKey)))
                .hasStatusOk().bodyJson().extractingPath("$.englishAuto").isEqualTo(false);
        assertThat(admin.put("/publications/" + id, """
                {"title": "Rapport d'activité de l'année 2026", "kind": "RAPPORT_ANNUEL", "publishedOn": "2026-10-01",
                 "published": true, "fileKey": "%s", "coverKey": "%s", "titleEn": "2026 Activity Report"}"""
                .formatted(fileKey, coverKey))).hasStatusOk();
        Thread.sleep(500);
        assertThat(admin.get("/publications/" + id)).bodyJson().extractingPath("$.titleEn").isEqualTo("2026 Activity Report");

        assertThat(admin.delete("/publications/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/public/publications"))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
    }

    @Test
    void publierSansPdfEstRefuseMaisUnBrouillonEstAccepte() {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");
        String body = """
                {"title": "Guide", "kind": "GUIDE", "publishedOn": "2026-10-01", "published": %s}""";
        assertThat(admin.post("/publications", body.formatted(true)))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("DOCUMENT_REQUIRED");
        assertThat(admin.post("/publications", body.formatted(false))).hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri("/api/v1/public/publications"))
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
    }

    @Test
    void seulsLesVraisPdfLisiblesSontAcceptes() throws Exception {
        AdminBrowser admin = AdminBrowser.login(mvc, "admin@anaqsup.test", "provisoire-1", "definitif-1");

        var renamed = new MockMultipartFile("file", "rapport.pdf", "application/pdf", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8));
        assertThat(admin.upload("/media/documents", renamed)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("UNSUPPORTED_DOCUMENT");

        var protectedPdf = new MockMultipartFile("file", "secret.pdf", "application/pdf", protectedPdf());
        assertThat(admin.upload("/media/documents", protectedPdf)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("PROTECTED_DOCUMENT");

        // Un PDF n'est pas une image : il ne peut pas servir de vignette.
        var upload = admin.upload("/media/documents", new MockMultipartFile("file", "a.pdf", "application/pdf", pdf("A")));
        assertThat(admin.post("/publications", """
                {"title": "Guide", "kind": "GUIDE", "publishedOn": "2026-10-01", "published": false, "coverKey": "%s"}"""
                .formatted(read(upload, "$.file.key"))))
                .hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.code").isEqualTo("UNKNOWN_MEDIA");
    }

    private static String read(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
    }

    private static byte[] pdf(String title) throws IOException {
        try (var document = new PDDocument()) {
            return save(document, title);
        }
    }

    private static byte[] protectedPdf() throws IOException {
        try (var document = new PDDocument()) {
            var policy = new StandardProtectionPolicy("proprietaire", "lecteur", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            return save(document, "Confidentiel");
        }
    }

    private static byte[] save(PDDocument document, String title) throws IOException {
        var page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        try (var content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 28);
            content.newLineAtOffset(72, 720);
            content.showText(title);
            content.endText();
        }
        var bytes = new ByteArrayOutputStream();
        document.save(bytes);
        return bytes.toByteArray();
    }
}
