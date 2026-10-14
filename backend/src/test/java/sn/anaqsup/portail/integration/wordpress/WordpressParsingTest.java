package sn.anaqsup.portail.integration.wordpress;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

/** Règles de lecture de l'ancien site, sur des exemples tirés d'anaqsup.sn. */
class WordpressParsingTest {

    @Test
    void anciennesCategoriesRangeesDansCellesDuNouveauSite() {
        assertThat(WordpressParsing.category(List.of("Partenariat"), "Atelier")).isEqualTo("cooperation-et-partenariats");
        assertThat(WordpressParsing.category(List.of("Actualité", "Visite"), "Atelier")).isEqualTo("cooperation-et-partenariats");
        assertThat(WordpressParsing.category(List.of("Rapports annuels"), "Atelier")).isEqualTo("communiques");
        assertThat(WordpressParsing.category(List.of("Discours du SE"), "Atelier")).isEqualTo("communiques");
        assertThat(WordpressParsing.category(List.of("Actualité"), "Atelier")).isEqualTo("vie-institutionnelle");
        assertThat(WordpressParsing.category(List.of("Non classée"), "Atelier")).isEqualTo("vie-institutionnelle");
        assertThat(WordpressParsing.category(List.of(), "Atelier")).isEqualTo("vie-institutionnelle");
    }

    @Test
    void appelsRangesDansAppelsACandidaturesQuelleQueSoitLeurCategorie() {
        for (String title : List.of("APPEL À MANIFESTATION D’INTÉRÊT, VAGUE 2019", "Avis à manifestation d’intérêt",
                "AVIS D’APPEL À CANDIDATURES FORMATION DIES", "Appel à candidature", "Appel à communication",
                "Recrutement des experts évaluateurs externes", "PROLONGATION POUR LA SOUMISSION DES PROGRAMMES",
                "Prorogation du délai de soumission de programmes")) {
            assertThat(WordpressParsing.category(List.of("Actualité"), title)).as(title).isEqualTo("appels-a-candidatures");
        }
        assertThat(WordpressParsing.category(List.of("Partenariat"), "Visite de l’ANAQ-Sup à l’UGB"))
                .isEqualTo("cooperation-et-partenariats");
    }

    @Test
    void identifiantEtAdresseDeLArticle() {
        assertThat(WordpressParsing.wordpressId("https://anaqsup.sn/?p=12345")).hasValue(12345);
        assertThat(WordpressParsing.wordpressId("https://anaqsup.sn/article/")).isEmpty();
        // Articles venus d'un ancien serveur : le guid est l'ancienne adresse, l'identifiant est sur la page.
        assertThat(WordpressParsing.wordpressId("http://35.180.23.236/visite-delegation-malienne/")).isEmpty();
        assertThat(WordpressParsing.pageId(Jsoup.parse("<body class=\"post-template-default single postid-998 elementor\"></body>")))
                .hasValue(998);
        assertThat(WordpressParsing.pageId(Jsoup.parse("<head><link rel=\"shortlink\" href=\"https://anaqsup.sn/?p=754\"></head>")))
                .hasValue(754);
        assertThat(WordpressParsing.slug("https://anaqsup.sn/session-extraordinaire-du-ca/?utm_source=rss&utm_medium=rss"))
                .isEqualTo("session-extraordinaire-du-ca");
        assertThat(WordpressParsing.slug("https://anaqsup.sn/r%C3%A9union-de-travail/")).isEqualTo("réunion-de-travail");
    }

    @Test
    void chapeauSansLaMentionDeLExtensionSeo() {
        String description = """
                <p>Le Conseil d&#8217;administration s&#8217;est réuni en session extraordinaire. [&hellip;]</p>
                <p>L’article <a href="https://anaqsup.sn/x/">Session extraordinaire</a> est apparu en premier sur <a href="https://anaqsup.sn">ANAQSUP</a>.</p>""";
        assertThat(WordpressParsing.summary(description)).isEqualTo("Le Conseil d’administration s’est réuni en session extraordinaire.");
        assertThat(WordpressParsing.summary("<p>" + "mot ".repeat(120) + "</p>")).hasSizeLessThanOrEqualTo(301).endsWith("…");
        // Variante réellement employée par anaqsup.sn, et simple appel au clic.
        assertThat(WordpressParsing.summary("""
                <p>Visite de travail.</p><p>The post <a href="#">Visite</a> first appeared on <a href="#">ANAQSUP</a>.</p>"""))
                .isEqualTo("Visite de travail.");
        assertThat(WordpressParsing.summary("<p>Cliquez ici pour visualiser ou Télécharger</p>")).isEmpty();
    }

    @Test
    void texteSansMentionNiParagraphesVides() {
        String body = WordpressParsing.body("""
                <p>Premier paragraphe.</p><p>&nbsp;</p><p></p>
                <p>The post <a href="#">X</a> appeared first on <a href="#">ANAQSUP</a>.</p>""");
        assertThat(body).contains("Premier paragraphe.").doesNotContain("appeared first").doesNotContain("<p></p>");
    }

    @Test
    void imagePrincipaleDeLaPlusGrandeVarianteRaisonnableALOriginal() {
        var page = Jsoup.parse("""
                <img class="attachment-full size-full wp-post-image" src="https://anaqsup.sn/u/CA.png"
                     srcset="https://anaqsup.sn/u/CA.png 2560w, https://anaqsup.sn/u/CA-300x200.png 300w,
                             https://anaqsup.sn/u/CA-1024x682.png 1024w, https://anaqsup.sn/u/CA-1536x1024.png 1536w">""");
        assertThat(WordpressParsing.coverCandidates(page)).containsExactly(
                "https://anaqsup.sn/u/CA-1536x1024.png", "https://anaqsup.sn/u/CA-1024x682.png", "https://anaqsup.sn/u/CA.png");
        assertThat(WordpressParsing.coverCandidates(Jsoup.parse("<p>Sans image</p>"))).isEmpty();
    }

    @Test
    void titreTypeEtDateDesDocuments() {
        assertThat(WordpressParsing.documentTitle("Référentiel d’évaluation institutionnelle (905.72 Ko)"))
                .isEqualTo("Référentiel d’évaluation institutionnelle");
        assertThat(WordpressParsing.documentTitle("Nouveau décret de l’ANAQ-Sup n°2018-1956 du 07 novembre 2018(1.29 Mo)"))
                .isEqualTo("Nouveau décret de l’ANAQ-Sup n°2018-1956 du 07 novembre 2018");

        assertThat(WordpressParsing.documentKind("textes-reglementaires", "Décret relatif aux diplômes")).isEqualTo("TEXTE_REGLEMENTAIRE");
        assertThat(WordpressParsing.documentKind("referentiels", "Guide d’auto-évaluation")).isEqualTo("GUIDE");
        assertThat(WordpressParsing.documentKind("publications", "Rapport d’activités 2022")).isEqualTo("RAPPORT_ANNUEL");
        assertThat(WordpressParsing.documentKind("publications", "Analyse situationnelle de la recherche")).isEqualTo("ETUDE");

        // Date écrite dans le titre, sinon dans le nom du fichier, sinon le mois de dépôt.
        assertThat(WordpressParsing.documentDate("Nouveau décret n°2018-1956 du 07 novembre 2018",
                "https://anaqsup.sn/wp-content/uploads/2023/07/decret.pdf")).hasValue(LocalDate.of(2018, 11, 7));
        assertThat(WordpressParsing.documentDate("Loi d’orientation relative aux ISEP",
                "https://anaqsup.sn/wp-content/uploads/2024/03/Loi-n°2024-01-du-11-janv-2024-relative-aux-ISEP.pdf"))
                .hasValue(LocalDate.of(2024, 1, 11));
        assertThat(WordpressParsing.documentDate("Référentiel programme inter-universitaire",
                "https://anaqsup.sn/wp-content/uploads/2024/07/Referentiel-prog.pdf")).hasValue(LocalDate.of(2024, 7, 1));
    }

    @Test
    void adressesAvecAccentsEncodeesUneSeuleFois() {
        assertThat(WordpressParsing.encode("https://anaqsup.sn/wp-content/uploads/2024/02/Décret-n°2021.pdf"))
                .isEqualTo("https://anaqsup.sn/wp-content/uploads/2024/02/D%C3%A9cret-n%C2%B02021.pdf");
        assertThat(WordpressParsing.encode("https://anaqsup.sn/uploads/Re%CC%81f-prog.pdf"))
                .isEqualTo("https://anaqsup.sn/uploads/Re%CC%81f-prog.pdf");
        assertThat(WordpressParsing.fileName("https://anaqsup.sn/uploads/Re%CC%81f-prog.pdf")).isEqualTo("Réf-prog.pdf");
    }

    @Test
    void premierPdfDUnArticlePublieCommeActualite() {
        assertThat(WordpressParsing.firstPdf("""
                <p><a href="/wp-content/uploads/2023/08/rapport-2014.pdf">Rapport d’activités 2014</a></p>
                <p><a href="https://anaqsup.sn/autre.PDF">Annexe</a></p>""", "https://anaqsup.sn/"))
                .contains("https://anaqsup.sn/wp-content/uploads/2023/08/rapport-2014.pdf");
        assertThat(WordpressParsing.firstPdf("<p>Rapport à venir.</p>", "https://anaqsup.sn/")).isEmpty();
    }
}
