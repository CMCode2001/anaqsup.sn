package sn.anaqsup.portail.referentiel.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Back-office : établissements (habilitation) et formations (accréditation), saisis tels quels. */
@Repository
public class AdminRegistryRepository {

    public record EstablishmentRow(UUID id, String slug, String name, @Nullable String acronym, String kind,
                                   @Nullable String city, @Nullable String region, @Nullable String website,
                                   @Nullable String avis, @Nullable Integer avisYear, long programs) {
    }

    public record EstablishmentChanges(String name, @Nullable String acronym, String kind, @Nullable String city,
                                       @Nullable String region, @Nullable String website, @Nullable String avis,
                                       @Nullable Integer avisYear) {
    }

    public record ProgramRow(UUID id, String slug, String title, String level, @Nullable String domain,
                             UUID establishmentId, String establishmentSlug, String establishmentName,
                             @Nullable String establishmentAcronym, @Nullable String avis, @Nullable Integer avisYear) {
    }

    public record ProgramChanges(String title, String level, @Nullable String domain, UUID establishmentId,
                                 @Nullable String avis, @Nullable Integer avisYear) {
    }

    public record Slice<T>(List<T> rows, long total) {
    }

    private final JdbcClient jdbc;

    AdminRegistryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // --------------------------------------------------------------------------------- établissements

    private static final String ESTABLISHMENT = """
            SELECT e.id, e.slug, e.name, e.acronym, e.kind, e.city, e.region, e.website, e.habilitation_avis,
                   e.habilitation_avis_year, (SELECT count(*) FROM program p WHERE p.establishment_id = e.id) AS programs
            FROM establishment e""";

    /** Recherche dans le nom, le sigle et la ville, sans accents ; ordre alphabétique. */
    public Slice<EstablishmentRow> establishments(@Nullable String q, @Nullable String avis, int page, int size) {
        long[] total = {0};
        List<EstablishmentRow> rows = jdbc.sql("""
                        WITH p AS (SELECT immutable_unaccent(lower(btrim(CAST(:q AS text)))) AS q, CAST(:avis AS text) AS avis)
                        SELECT x.*, count(*) OVER () AS total FROM (%s) x CROSS JOIN p
                        WHERE (p.q IS NULL OR p.q = ''
                               OR strpos(immutable_unaccent(lower(concat_ws(' ', x.name, x.acronym, x.city))), p.q) > 0)
                          AND (p.avis IS NULL OR (p.avis = 'AUCUN' AND x.habilitation_avis IS NULL) OR x.habilitation_avis = p.avis)
                        ORDER BY immutable_unaccent(lower(coalesce(x.acronym, x.name)))
                        LIMIT :size OFFSET :offset""".formatted(ESTABLISHMENT))
                .param("q", q).param("avis", avis).param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return establishment(rs);
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<EstablishmentRow> establishment(UUID id) {
        return jdbc.sql(ESTABLISHMENT + " WHERE e.id = :id").param("id", id).query((rs, n) -> establishment(rs)).optional();
    }

    public boolean establishmentSlugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM establishment WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    public UUID insertEstablishment(String slug, EstablishmentChanges c) {
        return bind(jdbc.sql("""
                        INSERT INTO establishment (slug, name, acronym, kind, city, region, website, habilitation_avis,
                                                   habilitation_avis_year)
                        VALUES (:slug, :name, :acronym, :kind, :city, :region, :website, :avis, :year)
                        RETURNING id"""), c)
                .param("slug", slug).query(UUID.class).single();
    }

    public void updateEstablishment(UUID id, EstablishmentChanges c) {
        bind(jdbc.sql("""
                        UPDATE establishment
                        SET name = :name, acronym = :acronym, kind = :kind, city = :city, region = :region,
                            website = :website, habilitation_avis = :avis, habilitation_avis_year = :year, updated_at = now()
                        WHERE id = :id"""), c)
                .param("id", id).update();
    }

    public void deleteEstablishment(UUID id) {
        jdbc.sql("DELETE FROM establishment WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec s, EstablishmentChanges c) {
        return s.param("name", c.name()).param("acronym", c.acronym()).param("kind", c.kind()).param("city", c.city())
                .param("region", c.region()).param("website", c.website()).param("avis", c.avis()).param("year", c.avisYear());
    }

    private static EstablishmentRow establishment(ResultSet rs) throws SQLException {
        return new EstablishmentRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("name"),
                rs.getString("acronym"), rs.getString("kind"), rs.getString("city"), rs.getString("region"),
                rs.getString("website"), rs.getString("habilitation_avis"), rs.getObject("habilitation_avis_year", Integer.class),
                rs.getLong("programs"));
    }

    // ------------------------------------------------------------------------------------- formations

    private static final String PROGRAM = """
            SELECT p.id, p.slug, p.title, p.level, p.domain, e.id AS establishment_id, e.slug AS establishment_slug,
                   e.name AS establishment_name, e.acronym AS establishment_acronym, p.accreditation_avis,
                   p.accreditation_avis_year
            FROM program p JOIN establishment e ON e.id = p.establishment_id""";

    public Slice<ProgramRow> programs(@Nullable String q, @Nullable UUID establishment, @Nullable String avis, int page, int size) {
        long[] total = {0};
        List<ProgramRow> rows = jdbc.sql("""
                        WITH f AS (SELECT immutable_unaccent(lower(btrim(CAST(:q AS text)))) AS q,
                                          CAST(:establishment AS uuid) AS establishment, CAST(:avis AS text) AS avis)
                        SELECT x.*, count(*) OVER () AS total FROM (%s) x CROSS JOIN f
                        WHERE (f.q IS NULL OR f.q = ''
                               OR strpos(immutable_unaccent(lower(concat_ws(' ', x.title, x.domain, x.establishment_name,
                                                                             x.establishment_acronym))), f.q) > 0)
                          AND (f.establishment IS NULL OR x.establishment_id = f.establishment)
                          AND (f.avis IS NULL OR (f.avis = 'AUCUN' AND x.accreditation_avis IS NULL) OR x.accreditation_avis = f.avis)
                        ORDER BY immutable_unaccent(lower(coalesce(x.establishment_acronym, x.establishment_name))),
                                 immutable_unaccent(lower(x.title))
                        LIMIT :size OFFSET :offset""".formatted(PROGRAM))
                .param("q", q).param("establishment", establishment).param("avis", avis)
                .param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return program(rs);
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<ProgramRow> program(UUID id) {
        return jdbc.sql(PROGRAM + " WHERE p.id = :id").param("id", id).query((rs, n) -> program(rs)).optional();
    }

    public boolean programSlugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM program WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    public UUID insertProgram(String slug, ProgramChanges c) {
        return bind(jdbc.sql("""
                        INSERT INTO program (slug, establishment_id, title, level, domain, accreditation_avis, accreditation_avis_year)
                        VALUES (:slug, :establishment, :title, :level, :domain, :avis, :year)
                        RETURNING id"""), c)
                .param("slug", slug).query(UUID.class).single();
    }

    public void updateProgram(UUID id, ProgramChanges c) {
        bind(jdbc.sql("""
                        UPDATE program
                        SET establishment_id = :establishment, title = :title, level = :level, domain = :domain,
                            accreditation_avis = :avis, accreditation_avis_year = :year, updated_at = now()
                        WHERE id = :id"""), c)
                .param("id", id).update();
    }

    public void deleteProgram(UUID id) {
        jdbc.sql("DELETE FROM program WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec s, ProgramChanges c) {
        return s.param("establishment", c.establishmentId()).param("title", c.title()).param("level", c.level())
                .param("domain", c.domain()).param("avis", c.avis()).param("year", c.avisYear());
    }

    private static ProgramRow program(ResultSet rs) throws SQLException {
        return new ProgramRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("title"), rs.getString("level"),
                rs.getString("domain"), rs.getObject("establishment_id", UUID.class), rs.getString("establishment_slug"),
                rs.getString("establishment_name"), rs.getString("establishment_acronym"), rs.getString("accreditation_avis"),
                rs.getObject("accreditation_avis_year", Integer.class));
    }
}
