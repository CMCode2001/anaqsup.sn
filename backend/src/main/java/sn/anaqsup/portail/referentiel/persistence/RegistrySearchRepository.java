package sn.anaqsup.portail.referentiel.persistence;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import sn.anaqsup.portail.shared.search.SearchHit;
import sn.anaqsup.portail.shared.search.SearchTerms;

/**
 * Recherche sur tout le site, côté registre : établissements et formations, sur leurs index plein texte
 * (sigle et nom d'abord, puis ville ; intitulé et domaine de la formation, puis son établissement).
 */
@Repository
public class RegistrySearchRepository {

    private static final String ESTABLISHMENTS = """
            WITH q AS (SELECT to_tsquery('french', immutable_unaccent(:tsquery)) AS query)
            SELECT e.slug, e.name AS title, NULL AS excerpt, concat_ws(' · ', e.acronym, e.city) AS label,
                   e.habilitation_avis AS code, count(*) OVER () AS total,
                   to_tsvector('french', immutable_unaccent(concat_ws(' ', e.acronym, e.name))) @@ q.query AS title_match,
                   ts_rank_cd(e.search_document, q.query, 1) AS rank
            FROM establishment e CROSS JOIN q
            WHERE e.search_document @@ q.query
            -- Même ordre que SearchHit.Scored.RELEVANCE (pas de date : le nom départage).
            ORDER BY title_match DESC, rank DESC, e.name
            LIMIT :size OFFSET :offset""";

    private static final String PROGRAMS = """
            WITH q AS (SELECT to_tsquery('french', immutable_unaccent(:tsquery)) AS query)
            SELECT p.slug, p.title, p.domain AS excerpt, coalesce(e.acronym, e.name) AS label,
                   p.accreditation_avis AS code, count(*) OVER () AS total,
                   to_tsvector('french', immutable_unaccent(p.title)) @@ q.query AS title_match,
                   ts_rank_cd(p.search_document || e.search_document, q.query, 1) AS rank
            FROM program p JOIN establishment e ON e.id = p.establishment_id CROSS JOIN q
            WHERE (p.search_document || e.search_document) @@ q.query
            ORDER BY title_match DESC, rank DESC, p.title
            LIMIT :size OFFSET :offset""";

    private final JdbcClient jdbc;

    RegistrySearchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public SearchHit.Slice establishments(SearchTerms terms, int page, int size) {
        return run(ESTABLISHMENTS, "establishments", terms, page, size);
    }

    public SearchHit.Slice programs(SearchTerms terms, int page, int size) {
        return run(PROGRAMS, "programs", terms, page, size);
    }

    private SearchHit.Slice run(String sql, String type, SearchTerms terms, int page, int size) {
        long[] total = {0};
        List<SearchHit.Scored> hits = new ArrayList<>(jdbc.sql(sql)
                .param("tsquery", terms.tsquery()).param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    // Noms propres, non traduits : servis tels quels dans les deux langues.
                    SearchHit hit = new SearchHit(type, rs.getString("slug"), rs.getString("title"), rs.getString("excerpt"),
                            null, rs.getString("label"), rs.getString("code"), null, null, "fr");
                    return new SearchHit.Scored(hit, rs.getBoolean("title_match"), rs.getDouble("rank"), null);
                })
                .list());
        return new SearchHit.Slice(hits, total[0]);
    }
}
