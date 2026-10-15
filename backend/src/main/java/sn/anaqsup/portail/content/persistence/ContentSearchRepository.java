package sn.anaqsup.portail.content.persistence;

import java.sql.Date;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import sn.anaqsup.portail.medias.MediaKeys;
import sn.anaqsup.portail.shared.search.SearchFilters;
import sn.anaqsup.portail.shared.search.SearchHit;
import sn.anaqsup.portail.shared.search.SearchTerms;

/**
 * Recherche sur tout le site, côté contenus publiés : actualités, événements, documents, pages. Le titre pèse plus
 * que le chapeau, lui-même plus que le texte ; les versions française et anglaise sont cherchées toutes les deux.
 */
@Repository
public class ContentSearchRepository {

    public enum Type { NEWS, EVENTS, PUBLICATIONS, PAGES }

    /** Titre, chapeau et lien dans la langue demandée, avec repli sur le français. */
    private static final String LOCALIZED = """
            CASE WHEN :en AND x.title_en IS NOT NULL THEN x.title_en ELSE x.title END AS title,
            CASE WHEN :en AND x.title_en IS NOT NULL THEN x.summary_en ELSE x.summary END AS excerpt,
            CASE WHEN :en AND x.title_en IS NOT NULL THEN 'en' ELSE 'fr' END AS lang,""";

    private static final String VISIBLE = "x.status = 'PUBLIE' AND x.published_at <= now()";

    /** Ce qui distingue un type : sa requête, le texte indexé et la date filtrée par la période. */
    private record Shape(String select, String document, String day) {
    }

    private static String document(String headline, String lead, String body) {
        return """
                setweight(to_tsvector('french', immutable_unaccent(concat_ws(' ', %s))), 'A')
                || setweight(to_tsvector('french', immutable_unaccent(concat_ws(' ', %s))), 'B')
                || setweight(to_tsvector('french', immutable_unaccent(regexp_replace(concat_ws(' ', %s), '<[^>]+>', ' ', 'g'))), 'D')"""
                .formatted(headline, lead, body);
    }

    private static final Shape NEWS = new Shape("""
            SELECT x.slug, d.doc, d.headline, %s CAST(x.published_at AS date) AS day,
                   CASE WHEN :en THEN coalesce(c.label_en, c.label) ELSE c.label END AS label, c.slug AS code,
                   NULL AS file_key, NULL AS file_url, x.cover_image_key AS image_key, x.published_at AS sort_date
            FROM news_item x JOIN news_category c ON c.id = x.category_id
            """.formatted(LOCALIZED),
            document("x.title, x.title_en", "x.summary, x.summary_en", "x.body_html, x.body_html_en"),
            "CAST(x.published_at AS date)");

    private static final Shape EVENTS = new Shape("""
            SELECT x.slug, d.doc, d.headline, %s CAST(x.starts_at AS date) AS day, x.location AS label, NULL AS code,
                   NULL AS file_key, NULL AS file_url, x.cover_image_key AS image_key, x.starts_at AS sort_date
            FROM event x
            """.formatted(LOCALIZED),
            document("x.title, x.title_en", "x.summary, x.summary_en, x.location", "x.body_html, x.body_html_en"),
            "CAST(x.starts_at AS date)");

    private static final Shape PUBLICATIONS = new Shape("""
            SELECT x.slug, d.doc, d.headline, %s x.published_on AS day, NULL AS label, x.kind AS code,
                   CAST(x.file_key AS text) AS file_key, x.file_url, CAST(x.cover_key AS text) AS image_key,
                   CAST(x.published_on AS timestamptz) AS sort_date
            FROM publication x
            """.formatted(LOCALIZED),
            document("x.title, x.title_en", "x.summary, x.summary_en", "''"),
            "x.published_on");

    private static final Shape PAGES = new Shape("""
            SELECT x.slug, d.doc, d.headline, %s NULL AS day, NULL AS label, NULL AS code,
                   NULL AS file_key, NULL AS file_url, NULL AS image_key, x.updated_at AS sort_date
            FROM institutional_page x
            """.formatted(LOCALIZED),
            document("x.title, x.title_en", "x.summary, x.summary_en", "x.body_html, x.body_html_en"),
            "NULL");

    private final JdbcClient jdbc;

    ContentSearchRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Filtres qu'un type sait appliquer : la période s'il est daté, la rubrique ou le type de document s'il en a. */
    public static boolean supports(Type type, SearchFilters filters) {
        return !(filters.dated() && type == Type.PAGES)
                && (filters.category() == null || type == Type.NEWS)
                && (filters.kind() == null || type == Type.PUBLICATIONS);
    }

    public SearchHit.Slice search(Type type, SearchTerms terms, SearchFilters filters, boolean english, int page, int size) {
        if (!supports(type, filters)) {
            throw new IllegalArgumentException("Filtre sans objet pour " + type);
        }
        Shape shape = switch (type) {
            case NEWS -> NEWS;
            case EVENTS -> EVENTS;
            case PUBLICATIONS -> PUBLICATIONS;
            case PAGES -> PAGES;
        };
        List<String> where = new ArrayList<>(List.of(VISIBLE, "d.doc @@ (SELECT query FROM q)"));
        if (filters.from() != null) where.add(shape.day() + " >= :from");
        if (filters.until() != null) where.add(shape.day() + " < :until");
        if (filters.category() != null) where.add("c.slug = :category");
        if (filters.kind() != null) where.add("x.kind = :kind");
        String sql = """
                WITH q AS (SELECT to_tsquery('french', immutable_unaccent(:tsquery)) AS query),
                     hits AS (%s CROSS JOIN LATERAL (SELECT %s AS doc,
                                                            to_tsvector('french', immutable_unaccent(concat_ws(' ', x.title, x.title_en))) AS headline) d
                              WHERE %s),
                     ranked AS (SELECT h.*, h.headline @@ (SELECT query FROM q) AS title_match,
                                       ts_rank_cd(h.doc, (SELECT query FROM q), 1) AS rank FROM hits h)
                SELECT ranked.*, count(*) OVER () AS total FROM ranked
                -- Même ordre que SearchHit.Scored.RELEVANCE, qui fusionne les types.
                ORDER BY title_match DESC, rank DESC, sort_date DESC NULLS LAST, title
                LIMIT :size OFFSET :offset""".formatted(shape.select(), shape.document(), String.join(" AND ", where));
        String typeName = type.name().toLowerCase(Locale.ROOT);
        long[] total = {0};
        List<SearchHit.Scored> hits = new ArrayList<>(jdbc.sql(sql)
                .param("tsquery", terms.tsquery()).param("en", english)
                .param("from", filters.from()).param("until", filters.until())
                .param("category", filters.category()).param("kind", filters.kind())
                .param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    Date day = rs.getDate("day");
                    String fileKey = rs.getString("file_key");
                    OffsetDateTime sortDate = rs.getObject("sort_date", OffsetDateTime.class);
                    SearchHit hit = new SearchHit(typeName, rs.getString("slug"), rs.getString("title"), rs.getString("excerpt"),
                            day == null ? null : day.toLocalDate(), rs.getString("label"), rs.getString("code"),
                            fileKey != null ? MediaKeys.publicUrl(fileKey) : rs.getString("file_url"),
                            MediaKeys.publicUrl(rs.getString("image_key")), rs.getString("lang"));
                    return new SearchHit.Scored(hit, rs.getBoolean("title_match"), rs.getDouble("rank"),
                            sortDate == null ? null : sortDate.toInstant());
                })
                .list());
        return new SearchHit.Slice(hits, total[0]);
    }
}
