package sn.anaqsup.portail.search.web;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.content.ContentSearch;
import sn.anaqsup.portail.referentiel.RegistrySearch;
import sn.anaqsup.portail.shared.search.SearchFilters;
import sn.anaqsup.portail.shared.search.SearchHit;
import sn.anaqsup.portail.shared.search.SearchTerms;
import sn.anaqsup.portail.shared.web.PageResponse;

/**
 * Recherche sur tout le site : une seule liste, tous types confondus, les plus pertinents d'abord (décision
 * ANAQ-Sup du 14/10/2026) ; {@code type} la restreint à une catégorie. Sert aussi les suggestions pendant la frappe.
 */
@RestController
class SiteSearchController {

    enum Type { NEWS, EVENTS, PUBLICATIONS, ESTABLISHMENTS, PROGRAMS, PAGES }

    private final ContentSearch content;
    private final RegistrySearch registry;
    private final Clock clock;

    SiteSearchController(ContentSearch content, RegistrySearch registry, Clock clock) {
        this.content = content;
        this.registry = registry;
        this.clock = clock;
    }

    @GetMapping("/api/v1/public/search")
    ResponseEntity<PageResponse<SearchHit>> search(
            @RequestParam @Size(min = 2, max = 100) String q,
            @RequestParam(defaultValue = "fr") @Pattern(regexp = "fr|en") String lang,
            @RequestParam(required = false) @Pattern(regexp = "news|events|publications|establishments|programs|pages") @Nullable String type,
            @RequestParam(required = false) @Pattern(regexp = "month|year|(19|20)\\d{2}") @Nullable String period,
            @RequestParam(required = false) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*") @Size(max = 160) @Nullable String category,
            @RequestParam(required = false) @Pattern(regexp = "RAPPORT_ANNUEL|REFERENTIEL|GUIDE|TEXTE_REGLEMENTAIRE|DISCOURS|ETUDE|AUTRE") @Nullable String kind,
            @RequestParam(defaultValue = "0") @Min(0) @Max(49) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        SearchFilters filters = filters(period, category, kind);
        Optional<SearchTerms> terms = SearchTerms.of(q);
        List<Type> types = (type == null ? Arrays.stream(Type.values()) : Stream.of(Type.valueOf(type.toUpperCase(Locale.ROOT))))
                .filter(t -> supports(t, filters))
                .toList();
        PageResponse<SearchHit> body = terms.isEmpty() || types.isEmpty()
                ? PageResponse.of(List.of(), page, size, 0)
                : merged(types, terms.get(), filters, lang.equals("en"), page, size);
        // Contenus publics, changeant peu : une minute de cache.
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic()).body(body);
    }

    /**
     * Chaque type fournit ses (page + 1) × size premiers résultats, déjà classés ; la fusion les reclasse ensemble
     * et garde la page demandée. Un seul type : sa page directement.
     */
    private PageResponse<SearchHit> merged(List<Type> types, SearchTerms terms, SearchFilters filters, boolean english,
                                           int page, int size) {
        if (types.size() == 1) {
            SearchHit.Slice slice = slice(types.getFirst(), terms, filters, english, page, size);
            return PageResponse.of(slice.items().stream().map(SearchHit.Scored::hit).toList(), page, size, slice.total());
        }
        int first = page * size;
        long total = 0;
        List<SearchHit.Scored> all = new ArrayList<>();
        for (Type type : types) {
            SearchHit.Slice slice = slice(type, terms, filters, english, 0, first + size);
            total += slice.total();
            all.addAll(slice.items());
        }
        all.sort(SearchHit.Scored.RELEVANCE);
        List<SearchHit> items = all.stream().skip(first).limit(size).map(SearchHit.Scored::hit).toList();
        return PageResponse.of(items, page, size, total);
    }

    /** Période : le dernier mois ou la dernière année (événements à venir compris), ou une année civile. */
    private SearchFilters filters(@Nullable String period, @Nullable String category, @Nullable String kind) {
        LocalDate today = LocalDate.now(clock);
        LocalDate from = null;
        LocalDate until = null;
        if ("month".equals(period)) {
            from = today.minusMonths(1);
        } else if ("year".equals(period)) {
            from = today.minusYears(1);
        } else if (period != null) {
            from = LocalDate.of(Integer.parseInt(period), 1, 1);
            until = from.plusYears(1);
        }
        return new SearchFilters(from, until, category, kind);
    }

    private boolean supports(Type type, SearchFilters filters) {
        return switch (type) {
            case NEWS -> content.supports(ContentSearch.Type.NEWS, filters);
            case EVENTS -> content.supports(ContentSearch.Type.EVENTS, filters);
            case PUBLICATIONS -> content.supports(ContentSearch.Type.PUBLICATIONS, filters);
            case PAGES -> content.supports(ContentSearch.Type.PAGES, filters);
            // Le registre n'a ni date, ni rubrique, ni type de document.
            case ESTABLISHMENTS, PROGRAMS -> filters.equals(SearchFilters.NONE);
        };
    }

    private SearchHit.Slice slice(Type type, SearchTerms terms, SearchFilters filters, boolean english, int page, int size) {
        return switch (type) {
            case NEWS -> content.search(ContentSearch.Type.NEWS, terms, filters, english, page, size);
            case EVENTS -> content.search(ContentSearch.Type.EVENTS, terms, filters, english, page, size);
            case PUBLICATIONS -> content.search(ContentSearch.Type.PUBLICATIONS, terms, filters, english, page, size);
            case PAGES -> content.search(ContentSearch.Type.PAGES, terms, filters, english, page, size);
            case ESTABLISHMENTS -> registry.establishments(terms, page, size);
            case PROGRAMS -> registry.programs(terms, page, size);
        };
    }
}
