package sn.anaqsup.portail.content.web;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.content.persistence.ContentRepository;
import sn.anaqsup.portail.content.persistence.ContentRepository.PublicationFilter;
import sn.anaqsup.portail.content.web.ContentResponses.Category;
import sn.anaqsup.portail.content.web.ContentResponses.Event;
import sn.anaqsup.portail.content.web.ContentResponses.EventSummary;
import sn.anaqsup.portail.content.web.ContentResponses.InstitutionalPage;
import sn.anaqsup.portail.content.web.ContentResponses.NewsItem;
import sn.anaqsup.portail.content.web.ContentResponses.NewsSummary;
import sn.anaqsup.portail.content.web.ContentResponses.Partner;
import sn.anaqsup.portail.content.web.ContentResponses.Publication;
import sn.anaqsup.portail.shared.web.PageResponse;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/** Site institutionnel : actualités, événements, publications et pages du contrat v1. */
@RestController
@RequestMapping("/api/v1/public")
class PublicContentController {

    private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";
    /** Langue demandée ; repli sur le français, contenu par contenu, si la traduction n'existe pas. */
    private static final String LANGS = "fr|en";
    private static final String KINDS = "RAPPORT_ANNUEL|REFERENTIEL|GUIDE|TEXTE_REGLEMENTAIRE|DISCOURS|ETUDE|AUTRE";
    private static final CacheControl PUBLIC_5_MIN = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final ContentRepository content;

    PublicContentController(ContentRepository content) {
        this.content = content;
    }

    @GetMapping("/news")
    ResponseEntity<PageResponse<NewsSummary>> news(
            @RequestParam(required = false) @Pattern(regexp = SLUG) @Nullable String category,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        var slice = content.news(category, english(lang), page, size);
        return ok(PageResponse.of(slice.rows().stream().map(NewsSummary::from).toList(), page, size, slice.total()));
    }

    @GetMapping("/news/categories")
    ResponseEntity<List<Category>> newsCategories(
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang) {
        return ok(content.categories(english(lang)).stream().map(row -> new Category(row.slug(), row.label())).toList());
    }

    @GetMapping("/news/{slug}")
    ResponseEntity<NewsItem> newsItem(
            @PathVariable @Pattern(regexp = SLUG) String slug,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang) {
        boolean en = english(lang);
        var row = content.newsItem(slug, en).orElseThrow(() -> new ResourceNotFoundException("Actualité", slug));
        return ok(NewsItem.from(row,
                ContentResponses.NewsLink.from(content.neighbour(row.slug(), row.publishedAt(), true, en)),
                ContentResponses.NewsLink.from(content.neighbour(row.slug(), row.publishedAt(), false, en))));
    }

    @GetMapping("/events")
    ResponseEntity<PageResponse<EventSummary>> events(
            @RequestParam(defaultValue = "upcoming") @Pattern(regexp = "upcoming|past") String period,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        var slice = content.events("upcoming".equals(period), english(lang), page, size);
        return ok(PageResponse.of(slice.rows().stream().map(EventSummary::from).toList(), page, size, slice.total()));
    }

    @GetMapping("/events/{slug}")
    ResponseEntity<Event> event(
            @PathVariable @Pattern(regexp = SLUG) String slug,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang) {
        return ok(content.event(slug, english(lang)).map(Event::from)
                .orElseThrow(() -> new ResourceNotFoundException("Événement", slug)));
    }

    @GetMapping("/publications")
    ResponseEntity<PageResponse<Publication>> publications(
            @RequestParam(required = false) @Size(max = 100) @Nullable String q,
            @RequestParam(required = false) @Pattern(regexp = KINDS) @Nullable String kind,
            @RequestParam(required = false) @Min(1900) @Max(2999) @Nullable Integer year,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        var slice = content.publications(new PublicationFilter(q, kind, year), english(lang), page, size);
        return ok(PageResponse.of(slice.rows().stream().map(Publication::from).toList(), page, size, slice.total()));
    }

    @GetMapping("/publications/years")
    ResponseEntity<List<Integer>> publicationYears(
            @RequestParam(required = false) @Pattern(regexp = KINDS) @Nullable String kind) {
        return ok(content.publicationYears(kind));
    }

    @GetMapping("/pages/{slug}")
    ResponseEntity<InstitutionalPage> page(
            @PathVariable @Pattern(regexp = SLUG) String slug,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = LANGS) String lang) {
        return ok(content.page(slug, english(lang)).map(InstitutionalPage::from)
                .orElseThrow(() -> new ResourceNotFoundException("Page", slug)));
    }

    @GetMapping("/partners")
    ResponseEntity<List<Partner>> partners() {
        return ok(content.partners().stream().map(Partner::from).toList());
    }

    private static boolean english(String lang) {
        return "en".equals(lang);
    }

    private static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().cacheControl(PUBLIC_5_MIN).body(body);
    }
}
