package sn.anaqsup.portail.content;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.anaqsup.portail.content.admin.HtmlSanitizer;
import sn.anaqsup.portail.content.persistence.LegacyImportRepository;
import sn.anaqsup.portail.shared.text.Slugs;

/**
 * Reprise des contenus de l'ancien site WordPress (utilisée par le module Intégrations) : actualités et
 * documents, publiés directement au nom de {@value #AUTHOR}. Chaque contenu n'est repris qu'une fois
 * (identifiant WordPress de l'article, adresse du PDF) : l'import peut être relancé sans doublon.
 */
@Service
public class LegacyContentImport {

    public static final String AUTHOR = "reprise.wordpress";

    /** Catégories d'actualités du site (migration V11). */
    public static final Set<String> CATEGORIES = Set.of("vie-institutionnelle", "assurance-qualite",
            "cooperation-et-partenariats", "appels-a-candidatures", "communiques");

    /** Types de document du site (contrat de l'API). */
    public static final Set<String> KINDS = Set.of("RAPPORT_ANNUEL", "REFERENTIEL", "GUIDE", "TEXTE_REGLEMENTAIRE",
            "DISCOURS", "ETUDE", "AUTRE");

    /**
     * @param slug     adresse d'origine (conservée pour les redirections), remplacée par le titre si elle n'est
     *                 pas conforme ; suffixée (-2, -3…) si elle est déjà prise
     * @param bodyHtml HTML d'origine, assaini ici par la même liste blanche que le back-office
     */
    public record Article(long wordpressId, String slug, String title, @Nullable String summary, String bodyHtml,
                          String category, Instant publishedAt, @Nullable UUID coverKey) {
    }

    public record Document(String legacyUrl, String title, String kind, LocalDate publishedOn, UUID fileKey,
                           @Nullable UUID coverKey) {
    }

    private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";

    private final LegacyImportRepository repository;
    private final ApplicationEventPublisher events;

    LegacyContentImport(LegacyImportRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    public boolean hasArticle(long wordpressId) {
        return repository.hasArticle(wordpressId);
    }

    public boolean hasDocument(String legacyUrl) {
        return repository.hasDocument(legacyUrl);
    }

    /** @return faux si l'article avait déjà été repris */
    @Transactional
    public boolean importArticle(Article article) {
        if (repository.hasArticle(article.wordpressId())) {
            return false;
        }
        if (!CATEGORIES.contains(article.category())) {
            throw new IllegalArgumentException("Catégorie inconnue : " + article.category());
        }
        String base = article.slug().matches(SLUG) ? article.slug() : Slugs.from(article.title());
        String slug = Slugs.unique(base, repository::articleSlugTaken);
        repository.insertArticle(article.wordpressId(), slug, repository.category(article.category()).orElseThrow(),
                article.title().strip(), article.summary(), HtmlSanitizer.clean(article.bodyHtml()), article.coverKey(),
                article.publishedAt(), AUTHOR);
        return true;
    }

    /** @return faux si le document avait déjà été repris */
    @Transactional
    public boolean importDocument(Document document) {
        if (repository.hasDocument(document.legacyUrl())) {
            return false;
        }
        if (!KINDS.contains(document.kind())) {
            throw new IllegalArgumentException("Type de document inconnu : " + document.kind());
        }
        String slug = Slugs.unique(Slugs.from(document.title()), repository::documentSlugTaken);
        repository.insertDocument(document.legacyUrl(), slug, document.title().strip(), document.kind(),
                document.publishedOn(), document.fileKey(), document.coverKey(), AUTHOR);
        return true;
    }

    /** Fin de reprise : le portail vide ses caches, la traduction automatique prend le relais. */
    @Transactional
    public void announce(List<String> tags) {
        events.publishEvent(new ContentChanged(tags));
    }
}
