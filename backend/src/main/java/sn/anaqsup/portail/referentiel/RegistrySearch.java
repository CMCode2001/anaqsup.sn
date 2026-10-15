package sn.anaqsup.portail.referentiel;

import org.springframework.stereotype.Service;

import sn.anaqsup.portail.referentiel.persistence.RegistrySearchRepository;
import sn.anaqsup.portail.shared.search.SearchHit;
import sn.anaqsup.portail.shared.search.SearchTerms;

/** Recherche sur tout le site (module Recherche), côté établissements et formations. */
@Service
public class RegistrySearch {

    private final RegistrySearchRepository repository;

    RegistrySearch(RegistrySearchRepository repository) {
        this.repository = repository;
    }

    public SearchHit.Slice establishments(SearchTerms terms, int page, int size) {
        return repository.establishments(terms, page, size);
    }

    public SearchHit.Slice programs(SearchTerms terms, int page, int size) {
        return repository.programs(terms, page, size);
    }
}
