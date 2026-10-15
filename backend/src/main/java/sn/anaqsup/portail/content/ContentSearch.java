package sn.anaqsup.portail.content;

import org.springframework.stereotype.Service;

import sn.anaqsup.portail.content.persistence.ContentSearchRepository;
import sn.anaqsup.portail.shared.search.SearchFilters;
import sn.anaqsup.portail.shared.search.SearchHit;
import sn.anaqsup.portail.shared.search.SearchTerms;

/** Recherche sur tout le site (module Recherche), côté contenus publiés. */
@Service
public class ContentSearch {

    public enum Type { NEWS, EVENTS, PUBLICATIONS, PAGES }

    private final ContentSearchRepository repository;

    ContentSearch(ContentSearchRepository repository) {
        this.repository = repository;
    }

    /** Le type sait-il appliquer ces filtres ? Sinon il est écarté des résultats. */
    public boolean supports(Type type, SearchFilters filters) {
        return ContentSearchRepository.supports(map(type), filters);
    }

    public SearchHit.Slice search(Type type, SearchTerms terms, SearchFilters filters, boolean english, int page, int size) {
        return repository.search(map(type), terms, filters, english, page, size);
    }

    private static ContentSearchRepository.Type map(Type type) {
        return ContentSearchRepository.Type.valueOf(type.name());
    }
}
