package sn.anaqsup.portail.referentiel.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Back-office : établissements et formations. Leurs avis ne sont plus saisis ici : ils reflètent la décision validée
 * la plus récente (déclencheurs de la migration V17, voir {@link DecisionRepository}).
 */
@Repository
public class AdminRegistryRepository {

    public record EstablishmentRow(UUID id, String slug, String name, @Nullable String acronym, String kind,
                                   @Nullable String city, @Nullable String region, @Nullable String website,
                                   @Nullable String avis, @Nullable Integer avisYear, long programs, String nature,
                                   @Nullable UUID parentId, boolean provisional) {
    }

    /** {@code nature} et {@code parentId} nuls : inchangés (le rattachement se retire par une route dédiée). */
    public record EstablishmentChanges(String name, @Nullable String acronym, String kind, @Nullable String city,
                                       @Nullable String region, @Nullable String website, @Nullable String nature,
                                       @Nullable UUID parentId) {
    }

    public record ProgramRow(UUID id, String slug, String title, String level, @Nullable String domain,
                             UUID establishmentId, String establishmentSlug, String establishmentName,
                             @Nullable String establishmentAcronym, @Nullable String avis, @Nullable Integer avisYear,
                             @Nullable String optionParcours, boolean provisional) {
    }

    public record ProgramChanges(String title, String level, @Nullable String domain, UUID establishmentId,
                                 @Nullable String optionParcours) {
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
                   e.habilitation_avis_year, (SELECT count(*) FROM program p WHERE p.establishment_id = e.id) AS programs,
                   e.nature, e.parent_id, e.verification_en_cours
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
                        INSERT INTO establishment (slug, name, acronym, kind, city, region, website, nature, parent_id)
                        VALUES (:slug, :name, :acronym, :kind, :city, :region, :website, coalesce(:nature, 'AUTRE'), :parent)
                        RETURNING id"""), c)
                .param("slug", slug).query(UUID.class).single();
    }

    public void updateEstablishment(UUID id, EstablishmentChanges c) {
        bind(jdbc.sql("""
                        UPDATE establishment
                        SET name = :name, acronym = :acronym, kind = :kind, city = :city, region = :region,
                            website = :website, nature = coalesce(:nature, nature), parent_id = coalesce(:parent, parent_id),
                            updated_at = now()
                        WHERE id = :id"""), c)
                .param("id", id).update();
    }

    public void deleteEstablishment(UUID id) {
        jdbc.sql("DELETE FROM establishment WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec s, EstablishmentChanges c) {
        return s.param("name", c.name()).param("acronym", c.acronym()).param("kind", c.kind()).param("city", c.city())
                .param("region", c.region()).param("website", c.website()).param("nature", c.nature())
                .param("parent", c.parentId());
    }

    /** Retire le rattachement d'une composante : elle redevient un établissement à part entière. */
    public void clearParent(UUID id) {
        jdbc.sql("UPDATE establishment SET parent_id = NULL, updated_at = now() WHERE id = :id").param("id", id).update();
    }

    /** L'établissement et ses composantes, à tous les niveaux : pour interdire qu'un parent soit sa propre composante. */
    public boolean isSelfOrDescendant(UUID root, UUID candidate) {
        return jdbc.sql("""
                        WITH RECURSIVE d AS (SELECT id FROM establishment WHERE id = :root
                                             UNION ALL SELECT c.id FROM establishment c JOIN d ON c.parent_id = d.id)
                        SELECT EXISTS (SELECT 1 FROM d WHERE id = :candidate)""")
                .param("root", root).param("candidate", candidate).query(Boolean.class).single();
    }

    public long componentCount(UUID id) {
        return jdbc.sql("SELECT count(*) FROM establishment WHERE parent_id = :id").param("id", id).query(Long.class).single();
    }

    public Optional<String> kindOf(UUID id) {
        return jdbc.sql("SELECT kind FROM establishment WHERE id = :id").param("id", id).query(String.class).optional();
    }

    private static EstablishmentRow establishment(ResultSet rs) throws SQLException {
        return new EstablishmentRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("name"),
                rs.getString("acronym"), rs.getString("kind"), rs.getString("city"), rs.getString("region"),
                rs.getString("website"), rs.getString("habilitation_avis"), rs.getObject("habilitation_avis_year", Integer.class),
                rs.getLong("programs"), rs.getString("nature"), rs.getObject("parent_id", UUID.class),
                rs.getBoolean("verification_en_cours"));
    }

    // ------------------------------------------------------------------------------------- formations

    private static final String PROGRAM = """
            SELECT p.id, p.slug, p.title, p.level, p.domain, e.id AS establishment_id, e.slug AS establishment_slug,
                   e.name AS establishment_name, e.acronym AS establishment_acronym, p.accreditation_avis,
                   p.accreditation_avis_year, p.option_parcours, p.verification_en_cours
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
                        INSERT INTO program (slug, establishment_id, title, level, domain, option_parcours)
                        VALUES (:slug, :establishment, :title, :level, :domain, :option)
                        RETURNING id"""), c)
                .param("slug", slug).query(UUID.class).single();
    }

    public void updateProgram(UUID id, ProgramChanges c) {
        bind(jdbc.sql("""
                        UPDATE program
                        SET establishment_id = :establishment, title = :title, level = :level, domain = :domain,
                            option_parcours = :option, updated_at = now()
                        WHERE id = :id"""), c)
                .param("id", id).update();
    }

    public void deleteProgram(UUID id) {
        jdbc.sql("DELETE FROM program WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec s, ProgramChanges c) {
        return s.param("establishment", c.establishmentId()).param("title", c.title()).param("level", c.level())
                .param("domain", c.domain()).param("option", c.optionParcours());
    }

    private static ProgramRow program(ResultSet rs) throws SQLException {
        return new ProgramRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("title"), rs.getString("level"),
                rs.getString("domain"), rs.getObject("establishment_id", UUID.class), rs.getString("establishment_slug"),
                rs.getString("establishment_name"), rs.getString("establishment_acronym"), rs.getString("accreditation_avis"),
                rs.getObject("accreditation_avis_year", Integer.class), rs.getString("option_parcours"),
                rs.getBoolean("verification_en_cours"));
    }
}
