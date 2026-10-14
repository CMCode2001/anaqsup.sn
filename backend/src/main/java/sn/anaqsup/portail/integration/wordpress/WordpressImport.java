package sn.anaqsup.portail.integration.wordpress;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;

import sn.anaqsup.portail.content.LegacyContentImport;
import sn.anaqsup.portail.content.LegacyContentImport.Article;
import sn.anaqsup.portail.medias.MediaStorage;

/**
 * Reprise des contenus de l'ancien site WordPress (décisions ANAQ-Sup du 14/10/2026) :
 * <ul>
 *   <li>actualités : flux RSS paginé (texte complet, date, catégories), image principale lue sur la page
 *       de chaque article et copiée dans la médiathèque ;</li>
 *   <li>documents : PDF des pages Textes réglementaires, Référentiels et Publications, copiés dans la
 *       médiathèque avec leur vignette (première page) ;</li>
 *   <li>articles publiés par erreur en « Communiqués » (rapports, discours) : repris comme documents, ou
 *       retirés (voir {@link Reclassement}).</li>
 * </ul>
 * L'API REST de l'ancien site est fermée (401) : seules ses pages publiques sont lues. L'import est relançable :
 * ce qui a déjà été repris est ignoré.
 *
 * <p>Lancement ponctuel (le programme s'arrête à la fin) :
 * {@code docker compose … run --rm backend --anaqsup.import.wordpress-url=https://anaqsup.sn}
 */
@Component
@ConditionalOnProperty(prefix = "anaqsup.import", name = "wordpress-url")
class WordpressImport implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WordpressImport.class);

    /** Pages de l'ancien site qui listent des documents. */
    private static final List<String> DOCUMENT_PAGES = List.of("textes-reglementaires", "referentiels", "publications");
    /** Requêtes simultanées vers l'ancien site : assez pour aller vite, peu pour ne pas le surcharger. */
    private static final int PARALLEL = 4;
    private static final int MAX_FEED_PAGES = 200;

    private final String site;
    private final boolean exitWhenDone;
    private final LegacyContentImport content;
    private final MediaStorage media;
    private final ConfigurableApplicationContext context;
    private final WordpressClient http = new WordpressClient();

    WordpressImport(@Value("${anaqsup.import.wordpress-url}") String site,
                    @Value("${anaqsup.import.exit-when-done:true}") boolean exitWhenDone,
                    LegacyContentImport content, MediaStorage media, ConfigurableApplicationContext context) {
        this.site = site.replaceAll("/+$", "");
        this.exitWhenDone = exitWhenDone;
        this.content = content;
        this.media = media;
        this.context = context;
    }

    /** Bilan affiché en fin de reprise. */
    private static final class Report {
        final AtomicInteger found = new AtomicInteger();
        final AtomicInteger imported = new AtomicInteger();
        final AtomicInteger already = new AtomicInteger();
        final AtomicInteger duplicates = new AtomicInteger();
        final AtomicInteger withoutImage = new AtomicInteger();
        final AtomicInteger removed = new AtomicInteger();
        final AtomicInteger failed = new AtomicInteger();
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Reprise WordPress depuis {} : début", site);
        Report articles = new Report();
        Report documents = new Report();
        Report misfiled = new Report();
        try {
            List<FeedItem> toReclassify = importArticles(articles);
            importDocuments(documents);
            List<String> tags = new ArrayList<>(List.of("news", "publications"));
            tags.addAll(reclassify(toReclassify, misfiled));
            content.announce(tags);
        } catch (RuntimeException e) {
            log.error("Reprise WordPress interrompue", e);
            articles.failed.incrementAndGet();
        }
        log.info("Reprise WordPress — actualités : {} trouvées, {} importées, {} déjà présentes, {} doublons ignorés, "
                        + "{} sans image, {} en échec", articles.found, articles.imported, articles.already,
                articles.duplicates, articles.withoutImage, articles.failed);
        log.info("Reprise WordPress — documents : {} trouvés, {} importés, {} déjà présents, {} doublons ignorés, {} en échec",
                documents.found, documents.imported, documents.already, documents.duplicates, documents.failed);
        log.info("Reprise WordPress — articles mal classés : {} trouvés, {} devenus documents, {} documents déjà présents, "
                        + "{} actualités retirées, {} en échec", misfiled.found, misfiled.imported, misfiled.already,
                misfiled.removed, misfiled.failed);
        if (exitWhenDone) {
            // Laisse le temps au webhook du portail (après validation) de partir avant l'arrêt.
            pause(Duration.ofSeconds(5));
            int failures = articles.failed.get() + documents.failed.get() + misfiled.failed.get();
            System.exit(SpringApplication.exit(context, () -> failures == 0 ? 0 : 1));
        }
    }

    // ------------------------------------------------------------------------------------- actualités

    /** {@code wordpressId} : null quand le guid est une ancienne adresse (lu alors sur la page de l'article). */
    private record FeedItem(@Nullable Long wordpressId, String link, String title, String descriptionHtml, String contentHtml,
                            List<String> categories, Instant publishedAt) {
    }

    /**
     * Article prêt à enregistrer : identifiant connu, image copiée (ou absente). {@code misfiled} : article de
     * {@link Reclassement}, à traiter comme un document (aucune image copiée).
     */
    private record Prepared(long wordpressId, @Nullable UUID cover, boolean already, boolean misfiled) {
    }

    /** @return les articles mal classés de l'ancien site (voir {@link Reclassement}), traités après les documents */
    private List<FeedItem> importArticles(Report report) {
        List<FeedItem> items = readFeed();
        report.found.set(items.size());
        log.info("Flux RSS lu : {} articles", items.size());

        // Doublons de l'ancien site (même titre, même date) : seul le premier est repris.
        Set<String> seen = new HashSet<>();
        List<FeedItem> todo = new ArrayList<>();
        List<FeedItem> misfiled = new ArrayList<>();
        for (FeedItem item : items) {
            if (!seen.add(WordpressParsing.plain(item.title()) + "|" + item.publishedAt())) {
                report.duplicates.incrementAndGet();
            } else if (item.wordpressId() != null && Reclassement.ACTIONS.containsKey(item.wordpressId())) {
                misfiled.add(item);
            } else if (item.wordpressId() != null && content.hasArticle(item.wordpressId())) {
                report.already.incrementAndGet();
            } else {
                todo.add(item);
            }
        }

        // Images en parallèle, enregistrement dans l'ordre chronologique : l'article d'origine garde son adresse.
        Map<FeedItem, Future<Prepared>> covers = new LinkedHashMap<>();
        Semaphore polite = new Semaphore(PARALLEL);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (FeedItem item : todo.reversed()) {
                covers.put(item, pool.submit(() -> {
                    polite.acquire();
                    try {
                        return prepare(item);
                    } finally {
                        polite.release();
                    }
                }));
            }
            int done = 0;
            for (var entry : covers.entrySet()) {
                FeedItem item = entry.getKey();
                try {
                    Prepared prepared = entry.getValue().get();
                    if (prepared.misfiled()) {
                        misfiled.add(new FeedItem(prepared.wordpressId(), item.link(), item.title(), item.descriptionHtml(),
                                item.contentHtml(), item.categories(), item.publishedAt()));
                        continue;
                    }
                    if (prepared.already()) {
                        report.already.incrementAndGet();
                        continue;
                    }
                    if (prepared.cover() == null) {
                        report.withoutImage.incrementAndGet();
                    }
                    boolean created = content.importArticle(new Article(prepared.wordpressId(), WordpressParsing.slug(item.link()),
                            item.title(), emptyToNull(WordpressParsing.summary(item.descriptionHtml())),
                            WordpressParsing.body(item.contentHtml()), WordpressParsing.category(item.categories()),
                            item.publishedAt(), prepared.cover()));
                    (created ? report.imported : report.already).incrementAndGet();
                } catch (Exception e) {
                    report.failed.incrementAndGet();
                    log.warn("Article non repris ({}) : {}", item.link(), e.toString());
                }
                if (++done % 50 == 0) {
                    log.info("Actualités : {} / {}", done, covers.size());
                }
            }
        }
        return misfiled;
    }

    private List<FeedItem> readFeed() {
        List<FeedItem> items = new ArrayList<>();
        DateTimeFormatter rfc1123 = DateTimeFormatter.RFC_1123_DATE_TIME;
        for (int page = 1; page <= MAX_FEED_PAGES; page++) {
            Document feed;
            try {
                feed = Jsoup.parse(http.text(site + "/feed/?paged=" + page), "", Parser.xmlParser());
            } catch (WordpressClient.NotFound e) {
                break; // au-delà de la dernière page
            } catch (IOException e) {
                throw new IllegalStateException("Flux RSS illisible (page " + page + ")", e);
            }
            var entries = feed.select("item");
            if (entries.isEmpty()) {
                break;
            }
            for (Element entry : entries) {
                var id = WordpressParsing.wordpressId(entry.select("guid").text());
                items.add(new FeedItem(id.isPresent() ? id.getAsLong() : null, entry.select("link").text(),
                        Parser.unescapeEntities(entry.select("title").text(), false),
                        entry.select("description").text(),
                        entry.getElementsByTag("content:encoded").stream().findFirst().map(Element::wholeText).orElse(""),
                        entry.select("category").eachText(),
                        ZonedDateTime.parse(entry.select("pubDate").text(), rfc1123).toInstant()));
            }
        }
        return items;
    }

    /**
     * Lit la page de l'article : identifiant (si le flux ne le donne pas) et image principale, copiée dans la
     * médiathèque. Un article déjà repris n'est pas retéléchargé.
     */
    private Prepared prepare(FeedItem item) throws IOException {
        Document page;
        try {
            page = Jsoup.parse(http.text(WordpressParsing.withoutQuery(item.link())), item.link());
        } catch (IOException e) {
            if (item.wordpressId() == null) {
                throw e; // sans la page, l'article n'a pas d'identifiant : il sera repris au prochain passage
            }
            log.warn("Page d'article illisible ({}) : {}", item.link(), e.getMessage());
            return new Prepared(item.wordpressId(), null, false, false);
        }
        Long id = item.wordpressId();
        if (id == null) {
            id = WordpressParsing.pageId(page).stream().boxed().findFirst()
                    .orElseThrow(() -> new IOException("Identifiant WordPress introuvable sur " + item.link()));
            if (Reclassement.ACTIONS.containsKey(id)) {
                return new Prepared(id, null, false, true);
            }
            if (content.hasArticle(id)) {
                return new Prepared(id, null, true, false);
            }
        }
        return new Prepared(id, cover(WordpressParsing.coverCandidates(page)), false, false);
    }

    /** Première variante de l'image principale acceptée par la médiathèque ; null si aucune. */
    private @Nullable UUID cover(List<String> candidates) {
        for (String url : candidates) {
            try {
                byte[] image = http.bytes(url, 5 * 1024 * 1024);
                return media.store(image, WordpressParsing.fileName(url), LegacyContentImport.AUTHOR).id();
            } catch (IOException | ErrorResponseException e) {
                log.debug("Variante d'image écartée ({}) : {}", url, e.getMessage());
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------------------- documents

    private record DocumentLink(String page, String url, String title) {
    }

    private void importDocuments(Report report) {
        // Une seule entrée par fichier, puis par titre (le même texte déposé deux fois : la version la plus récente).
        Map<String, DocumentLink> byUrl = new LinkedHashMap<>();
        for (String page : DOCUMENT_PAGES) {
            Document html;
            try {
                html = Jsoup.parse(http.text(site + "/" + page + "/"), site + "/");
            } catch (IOException e) {
                log.warn("Page {} illisible : {}", page, e.getMessage());
                report.failed.incrementAndGet();
                continue;
            }
            // Chaque PDF est lié deux fois (icône sans texte, puis titre) : on garde le lien titré.
            for (Element link : html.select("a[href~=(?i)\\.pdf$]")) {
                String title = WordpressParsing.documentTitle(link.text());
                if (!title.isBlank()) {
                    byUrl.putIfAbsent(link.absUrl("href"), new DocumentLink(page, link.absUrl("href"), title));
                }
            }
        }
        Map<String, DocumentLink> byTitle = new LinkedHashMap<>();
        for (DocumentLink link : byUrl.values()) {
            String key = WordpressParsing.plain(link.title());
            DocumentLink kept = byTitle.get(key);
            if (kept == null || link.url().compareTo(kept.url()) > 0) { // /uploads/AAAA/MM/ : le plus récent l'emporte
                if (kept != null) {
                    report.duplicates.incrementAndGet();
                }
                byTitle.put(key, link);
            } else {
                report.duplicates.incrementAndGet();
            }
        }
        report.found.set(byUrl.size());
        log.info("Documents trouvés : {} PDF ({} après dédoublonnage)", byUrl.size(), byTitle.size());

        for (DocumentLink link : byTitle.values()) {
            if (content.hasDocument(link.url())) {
                report.already.incrementAndGet();
                continue;
            }
            try {
                byte[] pdf = http.bytes(link.url(), 30 * 1024 * 1024);
                var stored = media.storeDocument(pdf, WordpressParsing.fileName(link.url()), LegacyContentImport.AUTHOR);
                var date = WordpressParsing.documentDate(link.title(), link.url()).orElse(java.time.LocalDate.now());
                boolean created = content.importDocument(new LegacyContentImport.Document(link.url(), link.title(),
                        WordpressParsing.documentKind(link.page(), link.title()), date, stored.file().id(),
                        stored.thumbnail() == null ? null : stored.thumbnail().id()));
                (created ? report.imported : report.already).incrementAndGet();
            } catch (IOException | RuntimeException e) {
                report.failed.incrementAndGet();
                log.warn("Document non repris ({}) : {}", link.url(), e.toString());
            }
        }
    }

    // ------------------------------------------------------------------------------------- articles mal classés

    /**
     * Rapports et discours publiés en actualités sur l'ancien site : repris comme documents (PDF copié, vignette),
     * sauf s'ils existent déjà ; l'actualité correspondante, si un import précédent l'avait reprise, est retirée.
     * Une actualité n'est retirée que si son document est bien en place.
     * @return étiquettes de cache des actualités retirées
     */
    private List<String> reclassify(List<FeedItem> items, Report report) {
        report.found.set(items.size());
        List<String> tags = new ArrayList<>();
        for (FeedItem item : items) {
            long id = Objects.requireNonNull(item.wordpressId());
            try {
                switch (Reclassement.ACTIONS.get(id)) {
                    case Reclassement.VersDocuments target -> moveToDocuments(item, target, report);
                    case Reclassement.Retire retire -> log.info("Article {} non repris : {}", item.link(), retire.motif());
                }
                content.removeArticle(id).ifPresent(slug -> {
                    tags.add("news:" + slug);
                    report.removed.incrementAndGet();
                });
            } catch (IOException | RuntimeException e) {
                report.failed.incrementAndGet();
                log.warn("Article mal classé non traité ({}) : {}", item.link(), e.toString());
            }
        }
        return tags;
    }

    private void moveToDocuments(FeedItem item, Reclassement.VersDocuments target, Report report) throws IOException {
        String title = target.title() != null ? target.title() : item.title().strip();
        String pdf = WordpressParsing.firstPdf(item.contentHtml(), site + "/")
                .orElseThrow(() -> new IOException("aucun PDF dans l'article"));
        if (content.hasDocument(pdf) || content.hasDocumentTitled(target.kind(), title)) {
            report.already.incrementAndGet();
            return;
        }
        byte[] bytes = http.bytes(pdf, 30 * 1024 * 1024);
        var stored = media.storeDocument(bytes, WordpressParsing.fileName(pdf), LegacyContentImport.AUTHOR);
        boolean created = content.importDocument(new LegacyContentImport.Document(pdf, title, target.kind(),
                LocalDate.ofInstant(item.publishedAt(), ZoneOffset.UTC), stored.file().id(),
                stored.thumbnail() == null ? null : stored.thumbnail().id()));
        (created ? report.imported : report.already).incrementAndGet();
    }

    // ------------------------------------------------------------------------------------- utilitaires

    private static @Nullable String emptyToNull(String text) {
        return text.isBlank() ? null : text;
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
