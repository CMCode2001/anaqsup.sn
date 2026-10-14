package sn.anaqsup.portail.integration.wordpress;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Règles de lecture de l'ancien site WordPress (fonctions pures, testées sans réseau) :
 * catégories, résumés, texte, image principale, titres, dates et types des documents.
 */
final class WordpressParsing {

    private WordpressParsing() {
    }

    // ------------------------------------------------------------------------------------- actualités

    /**
     * Correspondance des anciennes catégories (décision ANAQ-Sup du 14/10/2026) : Partenariat, Accord, Visite →
     * Coopération & partenariats ; Publication (rapports annuels, discours du SE…) → Communiqués ; Actualité,
     * Événements, Non classée → Vie institutionnelle. La coopération l'emporte si un article a plusieurs catégories.
     */
    /**
     * Appels de l'ANAQ-Sup (manifestations d'intérêt, candidatures, recrutements, prolongations de délai) : rubrique
     * « Appels à candidatures », quelle que soit leur catégorie d'origine (décision ANAQ-Sup du 14/10/2026).
     */
    private static final Pattern CALL = Pattern.compile(
            "appel a (manifestation|candidature|communication)|avis a manifestation|avis d'appel|recrutement|prolongation|prorogation");

    static String category(List<String> legacy, String title) {
        if (CALL.matcher(plain(title)).find()) {
            return "appels-a-candidatures";
        }
        List<String> names = legacy.stream().map(WordpressParsing::plain).toList();
        if (names.stream().anyMatch(n -> n.contains("partenariat") || n.contains("partanariat") || n.contains("accord")
                || n.contains("visite") || n.contains("cooperation"))) {
            return "cooperation-et-partenariats";
        }
        if (names.stream().anyMatch(n -> n.contains("publication") || n.contains("rapport") || n.contains("discours"))) {
            return "communiques";
        }
        return "vie-institutionnelle";
    }

    /** Identifiant WordPress, tiré du guid « https://anaqsup.sn/?p=12345 ». */
    static OptionalLong wordpressId(String guid) {
        Matcher m = Pattern.compile("[?&]p=(\\d+)").matcher(guid);
        return m.find() ? OptionalLong.of(Long.parseLong(m.group(1))) : OptionalLong.empty();
    }

    /**
     * Identifiant WordPress lu sur la page de l'article (classe {@code postid-998} ou lien court {@code ?p=998}) :
     * les articles venus d'un ancien serveur ont pour guid leur ancienne adresse, sans identifiant.
     */
    static OptionalLong pageId(Document page) {
        Matcher bodyClass = Pattern.compile("\\bpostid-(\\d+)\\b").matcher(page.body() == null ? "" : page.body().className());
        if (bodyClass.find()) {
            return OptionalLong.of(Long.parseLong(bodyClass.group(1)));
        }
        Element shortlink = page.selectFirst("link[rel=shortlink]");
        return shortlink == null ? OptionalLong.empty() : wordpressId(shortlink.attr("href"));
    }

    /** Dernier segment de l'adresse de l'article, sans paramètres de suivi (?utm_…). */
    static String slug(String link) {
        String path = URI.create(encode(link)).getPath();
        String[] segments = path.replaceAll("/+$", "").split("/");
        String last = segments.length == 0 ? "" : segments[segments.length - 1];
        return URLDecoder.decode(last, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
    }

    /** Adresse de l'article sans paramètres de suivi. */
    static String withoutQuery(String link) {
        int query = link.indexOf('?');
        return query < 0 ? link : link.substring(0, query);
    }

    /**
     * Chapeau à partir de l'extrait du flux : texte seul, sans la mention ajoutée par l'extension SEO
     * (« L'article … est apparu en premier sur ANAQSUP. », « The post … first appeared on ANAQSUP. ») ni « […] »,
     * coupé à 300 caractères.
     */
    static String summary(String descriptionHtml) {
        Document doc = Jsoup.parseBodyFragment(descriptionHtml);
        doc.select("p").stream().filter(WordpressParsing::isFeedFooter).forEach(Element::remove);
        String text = doc.text().replaceAll("\\s*\\[(…|&hellip;|\\.\\.\\.)]\\s*$", "").replaceAll("\\s*\\[…]", "").strip();
        // Un simple appel au clic (« Cliquez ici pour visualiser ou télécharger ») n'est pas un chapeau.
        if (plain(text).startsWith("cliquez ici")) {
            return "";
        }
        if (text.length() <= 300) {
            return text;
        }
        String cut = text.substring(0, 300);
        int space = cut.lastIndexOf(' ');
        return (space > 200 ? cut.substring(0, space) : cut).replaceAll("[\\s,;:.\\-–—]+$", "") + "…";
    }

    /** Texte de l'article, sans la mention de l'extension SEO ni les paragraphes vides (assaini ensuite). */
    static String body(String contentHtml) {
        Document doc = Jsoup.parseBodyFragment(contentHtml);
        doc.select("p").stream().filter(WordpressParsing::isFeedFooter).forEach(Element::remove);
        doc.select("p").stream().filter(p -> p.text().isBlank() && p.select("img, a").isEmpty()).forEach(Element::remove);
        return doc.body().html();
    }

    private static boolean isFeedFooter(Element paragraph) {
        String text = paragraph.text();
        return text.contains("est apparu en premier sur") || text.contains("appeared first on") || text.contains("first appeared on");
    }

    /**
     * Image principale d'une page d'article ({@code img.wp-post-image}) : de la plus grande variante de
     * 1 600 pixels de large au plus à la plus petite, puis l'original (le premier qui passe est retenu).
     */
    static List<String> coverCandidates(Document page) {
        Element img = page.selectFirst("img.wp-post-image");
        if (img == null) {
            return List.of();
        }
        record Candidate(String url, int width) {
        }
        List<Candidate> candidates = new ArrayList<>();
        for (String entry : img.attr("srcset").split(",")) {
            String[] parts = entry.strip().split("\\s+");
            if (parts.length == 2 && parts[1].endsWith("w")) {
                try {
                    candidates.add(new Candidate(parts[0], Integer.parseInt(parts[1].replace("w", ""))));
                } catch (NumberFormatException ignored) {
                    // descripteur inattendu : variante ignorée
                }
            }
        }
        List<String> urls = new ArrayList<>(candidates.stream()
                .filter(c -> c.width() <= 1600 && c.width() >= 600)
                .sorted(Comparator.comparingInt(Candidate::width).reversed())
                .map(Candidate::url).toList());
        if (!img.attr("src").isBlank() && !urls.contains(img.attr("src"))) {
            urls.add(img.attr("src"));
        }
        return urls;
    }

    // ------------------------------------------------------------------------------------- documents

    private static final Pattern SIZE = Pattern.compile("\\s*\\(\\s*[\\d.,]+\\s*(?:ko|mo|kb|mb|o)\\s*\\)\\s*$", Pattern.CASE_INSENSITIVE);

    /** Premier PDF lié dans le texte d'un article (document publié comme actualité), en adresse absolue. */
    static Optional<String> firstPdf(String contentHtml, String baseUrl) {
        return Jsoup.parse(contentHtml, baseUrl).select("a[href~=(?i)\\.pdf$]").stream()
                .map(link -> link.absUrl("href")).filter(url -> !url.isBlank()).findFirst();
    }

    /** « Référentiel d'évaluation institutionnelle (905.72 Ko) » → « Référentiel d'évaluation institutionnelle ». */
    static String documentTitle(String linkText) {
        return SIZE.matcher(linkText.replace(' ', ' ').replaceAll("\\s+", " ").strip()).replaceAll("").strip();
    }

    /**
     * Type d'un document selon la page de l'ancien site et son titre (un guide publié parmi les référentiels
     * reste un guide).
     */
    static String documentKind(String page, String title) {
        String t = plain(title);
        if (t.contains("guide")) {
            return "GUIDE";
        }
        return switch (page) {
            case "textes-reglementaires" -> "TEXTE_REGLEMENTAIRE";
            case "referentiels" -> "REFERENTIEL";
            default -> t.contains("rapport d'activite") || t.contains("rapport annuel") ? "RAPPORT_ANNUEL"
                    : t.contains("discours") || t.contains("allocution") ? "DISCOURS"
                    : "ETUDE";
        };
    }

    private static final Pattern FRENCH_DATE = Pattern.compile(
            "(\\d{1,2})(?:er)?[\\s_-]+(janv(?:ier)?|fevr?(?:ier)?|mars|avr(?:il)?|mai|juin|juil(?:let)?|aout|sept(?:embre)?"
                    + "|oct(?:obre)?|nov(?:embre)?|dec(?:embre)?)\\.?[\\s_-]+((?:19|20)\\d{2})");
    private static final Pattern UPLOAD_MONTH = Pattern.compile("/uploads/((?:19|20)\\d{2})/(\\d{2})/");

    /**
     * Date du document : la date écrite dans le titre ou le nom du fichier (« Loi n°2024-01 du 11 janv. 2024 »),
     * sinon le mois de dépôt sur l'ancien site (/uploads/2024/03/ → 1er mars 2024).
     */
    static Optional<LocalDate> documentDate(String title, String url) {
        String fileName = URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
        for (String text : List.of(title, fileName)) {
            Matcher m = FRENCH_DATE.matcher(plain(text));
            if (m.find()) {
                try {
                    return Optional.of(LocalDate.of(Integer.parseInt(m.group(3)), month(m.group(2)), Integer.parseInt(m.group(1))));
                } catch (java.time.DateTimeException ignored) {
                    // date impossible (ex. 31 juin) : on essaie la suite
                }
            }
        }
        Matcher upload = UPLOAD_MONTH.matcher(url);
        return upload.find()
                ? Optional.of(LocalDate.of(Integer.parseInt(upload.group(1)), Integer.parseInt(upload.group(2)), 1))
                : Optional.empty();
    }

    private static int month(String name) {
        if (name.startsWith("juil")) {
            return 7;
        }
        List<String> prefixes = List.of("jan", "fev", "mar", "avr", "mai", "juin", "juil", "aou", "sep", "oct", "nov", "dec");
        for (int i = 0; i < prefixes.size(); i++) {
            if (name.startsWith(prefixes.get(i))) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException(name);
    }

    /** Nom du fichier tel qu'il sera proposé au téléchargement (accents recomposés : « Réf », pas « Re´f »). */
    static String fileName(String url) {
        return Normalizer.normalize(URLDecoder.decode(url.substring(url.lastIndexOf('/') + 1), StandardCharsets.UTF_8),
                Normalizer.Form.NFC);
    }

    // ------------------------------------------------------------------------------------- utilitaires

    /** Minuscules sans accents, apostrophes droites : pour comparer des libellés. */
    static String plain(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('’', '\'').toLowerCase(Locale.ROOT);
    }

    /**
     * Adresse utilisable par le client HTTP : les caractères non ASCII et les espaces sont encodés,
     * les séquences déjà encodées (%CC%81) restent telles quelles.
     */
    static String encode(String href) {
        var out = new StringBuilder();
        for (byte b : href.strip().getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if (c > 0x7E || c <= 0x20 || c == '"' || c == '<' || c == '>' || c == '\\' || c == '^' || c == '`'
                    || c == '{' || c == '|' || c == '}') {
                out.append('%').append(String.format("%02X", c));
            } else {
                out.append((char) c);
            }
        }
        return out.toString();
    }
}
