package sn.anaqsup.portail.referentiel.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import sn.anaqsup.portail.referentiel.ReferentielQueries;

/**
 * Listes des habilitations et accréditations (ADR-0005 pour la recherche) : plein texte français sans accents, trigrammes pour les
 * fautes de frappe, et correspondance exacte de sigle en tête des résultats.
 * Chaque paramètre est typé une seule fois (CTE {@code p}) : PostgreSQL n'a jamais à deviner.
 */
@Repository
public class RegistryRepository implements ReferentielQueries {

    public record EstablishmentRow(UUID id, String slug, String name, @Nullable String acronym, String kind,
                                   @Nullable String city, @Nullable String region, @Nullable String website,
                                   @Nullable String avis, @Nullable Integer avisYear) {
    }

    public record ProgramRow(UUID id, String slug, String title, String level, @Nullable String domain,
                             String establishmentSlug, String establishmentName,
                             @Nullable String avis, @Nullable Integer avisYear) {
    }

    public record Slice<T>(List<T> rows, long total) {
    }

    private static final String SEARCH_ESTABLISHMENTS = """
            WITH p AS (SELECT CAST(:q AS text) AS q, CAST(:city AS text) AS city, CAST(:kind AS text) AS kind,
                              CAST(:avis AS text) AS avis),
                 t AS (SELECT p.*,
                              CASE WHEN p.q IS NULL THEN NULL ELSE websearch_to_tsquery('french', immutable_unaccent(p.q)) END AS query,
                              immutable_unaccent(lower(p.q)) AS nq
                       FROM p)
            SELECT e.id, e.slug, e.name, e.acronym, e.kind, e.city, e.region, e.website,
                   e.habilitation_avis AS avis, e.habilitation_avis_year AS avis_year, count(*) OVER () AS total
            FROM establishment e
                     CROSS JOIN t
                     LEFT JOIN LATERAL (SELECT EXISTS (SELECT 1 FROM establishment_alias a
                                                       WHERE a.establishment_id = e.id
                                                         AND immutable_unaccent(lower(a.alias)) = t.nq)
                                               OR immutable_unaccent(lower(e.acronym)) = t.nq AS exact) m ON true
            WHERE (t.q IS NULL OR m.exact OR e.search_document @@ t.query OR immutable_unaccent(lower(e.name)) % t.nq)
              AND (t.city IS NULL OR immutable_unaccent(lower(e.city)) = immutable_unaccent(lower(t.city)))
              AND (t.kind IS NULL OR e.kind = t.kind)
              AND (t.avis IS NULL OR e.habilitation_avis = t.avis)
            ORDER BY coalesce(m.exact, false) DESC,
                     CASE WHEN t.query IS NULL THEN 0 ELSE ts_rank_cd(e.search_document, t.query) END DESC,
                     CASE WHEN t.nq IS NULL THEN 0 ELSE similarity(immutable_unaccent(lower(e.name)), t.nq) END DESC,
                     e.name
            LIMIT :size OFFSET :offset
            """;

    private static final String SEARCH_PROGRAMS = """
            WITH p AS (SELECT CAST(:q AS text) AS q, CAST(:establishment AS text) AS establishment, CAST(:level AS text) AS level,
                              CAST(:avis AS text) AS avis),
                 t AS (SELECT p.*,
                              CASE WHEN p.q IS NULL THEN NULL ELSE websearch_to_tsquery('french', immutable_unaccent(p.q)) END AS query,
                              immutable_unaccent(lower(p.q)) AS nq
                       FROM p)
            SELECT pr.id, pr.slug, pr.title, pr.level, pr.domain, e.slug AS establishment_slug, e.name AS establishment_name,
                   pr.accreditation_avis AS avis, pr.accreditation_avis_year AS avis_year, count(*) OVER () AS total
            FROM program pr
                     JOIN establishment e ON e.id = pr.establishment_id
                     CROSS JOIN t
            WHERE (t.q IS NULL
                OR (pr.search_document || e.search_document) @@ t.query
                OR immutable_unaccent(lower(pr.title)) % t.nq)
              AND (t.establishment IS NULL OR e.slug = t.establishment)
              AND (t.level IS NULL OR pr.level = t.level)
              AND (t.avis IS NULL OR pr.accreditation_avis = t.avis)
            ORDER BY CASE WHEN t.query IS NULL THEN 0 ELSE ts_rank_cd(pr.search_document || e.search_document, t.query) END DESC,
                     CASE WHEN t.nq IS NULL THEN 0 ELSE similarity(immutable_unaccent(lower(pr.title)), t.nq) END DESC,
                     pr.title
            LIMIT :size OFFSET :offset
            """;

    private static final String PROGRAM_COLUMNS = """
            SELECT pr.id, pr.slug, pr.title, pr.level, pr.domain, e.slug AS establishment_slug, e.name AS establishment_name,
                   pr.accreditation_avis AS avis, pr.accreditation_avis_year AS avis_year
            FROM program pr JOIN establishment e ON e.id = pr.establishment_id
            """;

    private final JdbcClient jdbc;

    RegistryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Slice<EstablishmentRow> searchEstablishments(@Nullable String q, @Nullable String city, @Nullable String kind,
                                                        @Nullable String avis, int page, int size) {
        long[] total = {0};
        List<EstablishmentRow> rows = jdbc.sql(SEARCH_ESTABLISHMENTS)
                .param("q", q).param("city", city).param("kind", kind).param("avis", avis)
                .param("size", size).param("offset", (long) page * size)
                .query((rs, rowNum) -> {
                    total[0] = rs.getLong("total");
                    return establishment(rs);
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<EstablishmentRow> findEstablishment(String slug) {
        return jdbc.sql("""
                        SELECT id, slug, name, acronym, kind, city, region, website,
                               habilitation_avis AS avis, habilitation_avis_year AS avis_year
                        FROM establishment WHERE slug = :slug""")
                .param("slug", slug)
                .query((rs, rowNum) -> establishment(rs))
                .optional();
    }

    public long countPrograms(UUID establishmentId) {
        return jdbc.sql("SELECT count(*) FROM program WHERE establishment_id = :id")
                .param("id", establishmentId).query(Long.class).single();
    }

    public Slice<ProgramRow> searchPrograms(@Nullable String q, @Nullable String establishmentSlug, @Nullable String level,
                                            @Nullable String avis, int page, int size) {
        long[] total = {0};
        List<ProgramRow> rows = jdbc.sql(SEARCH_PROGRAMS)
                .param("q", q).param("establishment", establishmentSlug).param("level", level).param("avis", avis)
                .param("size", size).param("offset", (long) page * size)
                .query((rs, rowNum) -> {
                    total[0] = rs.getLong("total");
                    return program(rs);
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<ProgramRow> findProgram(String slug) {
        return jdbc.sql(PROGRAM_COLUMNS + " WHERE pr.slug = :slug")
                .param("slug", slug)
                .query((rs, rowNum) -> program(rs))
                .optional();
    }

    @Override
    public long countFavorableHabilitations() {
        return jdbc.sql("SELECT count(*) FROM establishment WHERE habilitation_avis = 'FAVORABLE'").query(Long.class).single();
    }

    @Override
    public long countFavorableAccreditations() {
        return jdbc.sql("SELECT count(*) FROM program WHERE accreditation_avis = 'FAVORABLE'").query(Long.class).single();
    }

    private static EstablishmentRow establishment(ResultSet rs) throws SQLException {
        return new EstablishmentRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("name"),
                rs.getString("acronym"), rs.getString("kind"), rs.getString("city"), rs.getString("region"),
                rs.getString("website"), rs.getString("avis"), rs.getObject("avis_year", Integer.class));
    }

    private static ProgramRow program(ResultSet rs) throws SQLException {
        return new ProgramRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("title"),
                rs.getString("level"), rs.getString("domain"), rs.getString("establishment_slug"),
                rs.getString("establishment_name"), rs.getString("avis"), rs.getObject("avis_year", Integer.class));
    }
}
