package sn.anaqsup.portail.referentiel.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Décisions du Conseil scientifique et rapports d'évaluation (migration V17).
 * <p>
 * Le statut courant n'est pas calculé ici : les déclencheurs de la base recopient la décision validée la plus
 * récente dans les colonnes d'avis des fiches (établissement, formation). Ce dépôt lit l'historique et écrit les décisions.
 */
@Repository
public class DecisionRepository {

    // ------------------------------------------------------------------------------------ lecture publique

    /** Rapport d'évaluation public : un rapport privé n'est jamais lu par les requêtes publiques. */
    public record PublicReport(String title, String url, @Nullable LocalDate signedOn, @Nullable Boolean linkOk) {
    }

    /**
     * Une décision dans l'historique d'un établissement ou d'une formation.
     * {@code avis} : celui de la formation, ou l'avis d'ensemble (« MIXTE » quand les formations n'ont pas toutes le même).
     */
    public record PublicEntry(UUID id, String kind, int year, @Nullable LocalDate decidedOn, int validityYears,
                              LocalDate validUntil, boolean validUntilEstimated, boolean inForce, @Nullable String avis,
                              @Nullable Integer wave, @Nullable PublicReport report) {
    }

    private static final String VALID_UNTIL = """
            ((coalesce(d.decided_on, make_date(d.display_year, 12, 31)) + make_interval(years => d.validity_years))::date - 1)""";

    private static final String REFERENCE_DATE = "coalesce(d.decided_on, make_date(d.display_year, 12, 31))";

    private static final String EFFECTIVE_AVIS = """
            coalesce(d.avis, (SELECT CASE WHEN count(DISTINCT x.avis) = 0 THEN NULL
                                          WHEN count(DISTINCT x.avis) = 1 THEN min(x.avis) ELSE 'MIXTE' END
                              FROM decision_program x WHERE x.decision_id = d.id))""";

    private static final String PUBLIC_COLUMNS = """
            d.id, d.kind, d.display_year, d.decided_on, d.validity_years, %s AS valid_until, (d.decided_on IS NULL) AS estimated,
            (%s >= current_date) AS in_force, d.wave,
            r.title AS r_title, r.url AS r_url, r.signed_on AS r_signed_on, r.link_ok AS r_link_ok
            """.formatted(VALID_UNTIL, VALID_UNTIL);

    private final JdbcClient jdbc;

    DecisionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Décisions validées d'habilitation d'un établissement, de la plus récente à la plus ancienne. */
    public List<PublicEntry> establishmentHistory(UUID establishmentId) {
        return jdbc.sql("SELECT " + PUBLIC_COLUMNS + ", " + EFFECTIVE_AVIS + """
                         AS avis
                        FROM decision d
                                 LEFT JOIN report r ON r.id = d.report_id AND r.visibility = 'PUBLIC'
                        WHERE d.establishment_id = :id AND d.status = 'VALIDE' AND d.kind IN ('HABILITATION', 'HABILITATION_COMPLEMENTAIRE')
                        ORDER BY\s""" + REFERENCE_DATE + " DESC, d.created_at DESC")
                .param("id", establishmentId).query((rs, n) -> publicEntry(rs)).list();
    }

    /** Décisions validées qui concernent une formation, avec l'avis rendu pour elle et son rapport. */
    public List<PublicEntry> programHistory(UUID programId) {
        return jdbc.sql("SELECT " + PUBLIC_COLUMNS + """
                        , dp.avis AS avis
                        FROM decision_program dp
                                 JOIN decision d ON d.id = dp.decision_id AND d.status = 'VALIDE'
                                 LEFT JOIN report r ON r.id = coalesce(dp.report_id, d.report_id) AND r.visibility = 'PUBLIC'
                        WHERE dp.program_id = :id
                        ORDER BY\s""" + REFERENCE_DATE + " DESC, d.created_at DESC")
                .param("id", programId).query((rs, n) -> publicEntry(rs)).list();
    }

    private static PublicEntry publicEntry(ResultSet rs) throws SQLException {
        PublicReport report = rs.getString("r_url") == null ? null
                : new PublicReport(rs.getString("r_title"), rs.getString("r_url"), rs.getObject("r_signed_on", LocalDate.class),
                rs.getObject("r_link_ok", Boolean.class));
        return new PublicEntry(rs.getObject("id", UUID.class), rs.getString("kind"), rs.getInt("display_year"),
                rs.getObject("decided_on", LocalDate.class), rs.getInt("validity_years"),
                rs.getObject("valid_until", LocalDate.class), rs.getBoolean("estimated"), rs.getBoolean("in_force"),
                rs.getString("avis"), rs.getObject("wave", Integer.class), report);
    }

    // ------------------------------------------------------------------------------------- back-office

    public record ReportRow(UUID id, String kind, String title, String url, @Nullable LocalDate signedOn,
                            @Nullable LocalDate visitedOn, String visibility, @Nullable Boolean linkOk) {
    }

    public record DecisionRow(UUID id, String kind, UUID establishmentId, String establishmentSlug, String establishmentName,
                              @Nullable LocalDate decidedOn, int displayYear, int validityYears, LocalDate validUntil,
                              boolean validUntilEstimated, @Nullable Integer wave, @Nullable String avis,
                              @Nullable String effectiveAvis, @Nullable UUID reportId, @Nullable UUID previousId, String status,
                              @Nullable String comment, String enteredBy, @Nullable String validatedBy,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    public record ProgramAvisRow(UUID programId, String programSlug, String title, String avis, @Nullable UUID reportId,
                                 @Nullable String remark) {
    }

    public record ReportInput(String kind, String title, String url, @Nullable LocalDate signedOn,
                              @Nullable LocalDate visitedOn, String visibility) {
    }

    public record ProgramAvisInput(UUID programId, String avis, @Nullable UUID reportId, @Nullable String remark) {
    }

    public record DecisionInput(String kind, UUID establishmentId, @Nullable LocalDate decidedOn, int displayYear,
                                int validityYears, @Nullable Integer wave, @Nullable String avis, @Nullable UUID reportId,
                                @Nullable UUID previousId, @Nullable String comment) {
    }

    public record Slice<T>(List<T> rows, long total) {
    }

    private static final String DECISION_COLUMNS = """
            SELECT d.id, d.kind, d.establishment_id, e.slug AS establishment_slug, e.name AS establishment_name, d.decided_on,
                   d.display_year, d.validity_years, %s AS valid_until, (d.decided_on IS NULL) AS estimated, d.wave, d.avis,
                   %s AS effective_avis, d.report_id, d.previous_id, d.status, d.comment, d.entered_by, d.validated_by,
                   d.created_at, d.updated_at
            FROM decision d JOIN establishment e ON e.id = d.establishment_id
            """.formatted(VALID_UNTIL, EFFECTIVE_AVIS);

    public Slice<DecisionRow> decisions(@Nullable UUID establishment, @Nullable String status, @Nullable String kind,
                                        int page, int size) {
        long[] total = {0};
        List<DecisionRow> rows = jdbc.sql("""
                        SELECT x.*, count(*) OVER () AS total FROM (%s) x
                        WHERE (CAST(:establishment AS uuid) IS NULL OR x.establishment_id = CAST(:establishment AS uuid))
                          AND (CAST(:status AS text) IS NULL OR x.status = CAST(:status AS text))
                          AND (CAST(:kind AS text) IS NULL OR x.kind = CAST(:kind AS text))
                        ORDER BY x.updated_at DESC, x.id
                        LIMIT :size OFFSET :offset""".formatted(DECISION_COLUMNS))
                .param("establishment", establishment).param("status", status).param("kind", kind)
                .param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return decision(rs);
                }).list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<DecisionRow> decision(UUID id) {
        return jdbc.sql(DECISION_COLUMNS + " WHERE d.id = :id").param("id", id).query((rs, n) -> decision(rs)).optional();
    }

    public List<ProgramAvisRow> decisionPrograms(UUID decisionId) {
        return jdbc.sql("""
                        SELECT dp.program_id, p.slug, p.title, dp.avis, dp.report_id, dp.remark
                        FROM decision_program dp JOIN program p ON p.id = dp.program_id
                        WHERE dp.decision_id = :id ORDER BY immutable_unaccent(lower(p.title))""")
                .param("id", decisionId)
                .query((rs, n) -> new ProgramAvisRow(rs.getObject("program_id", UUID.class), rs.getString("slug"),
                        rs.getString("title"), rs.getString("avis"), rs.getObject("report_id", UUID.class), rs.getString("remark")))
                .list();
    }

    public Optional<ReportRow> report(UUID id) {
        return jdbc.sql("SELECT id, kind, title, url, signed_on, visited_on, visibility, link_ok FROM report WHERE id = :id")
                .param("id", id)
                .query((rs, n) -> new ReportRow(rs.getObject("id", UUID.class), rs.getString("kind"), rs.getString("title"),
                        rs.getString("url"), rs.getObject("signed_on", LocalDate.class), rs.getObject("visited_on", LocalDate.class),
                        rs.getString("visibility"), rs.getObject("link_ok", Boolean.class)))
                .optional();
    }

    /** Date de signature d'un rapport déjà connu (identifié par son adresse). */
    public Optional<LocalDate> knownReportSignature(String url) {
        return jdbc.sql("SELECT signed_on FROM report WHERE url = :url AND signed_on IS NOT NULL").param("url", url)
                .query(LocalDate.class).optional();
    }

    /** Un rapport est identifié par son adresse : s'il existe déjà il est réutilisé tel quel, sinon il est créé. */
    public UUID findOrCreateReport(ReportInput r) {
        return jdbc.sql("""
                        INSERT INTO report (kind, title, url, signed_on, visited_on, visibility)
                        VALUES (:kind, :title, :url, :signedOn, :visitedOn, :visibility)
                        ON CONFLICT (url) DO UPDATE SET url = EXCLUDED.url
                        RETURNING id""")
                .param("kind", r.kind()).param("title", r.title()).param("url", r.url()).param("signedOn", r.signedOn())
                .param("visitedOn", r.visitedOn()).param("visibility", r.visibility())
                .query(UUID.class).single();
    }

    /** Texte sans accents ni majuscules, ponctuation remplacée par des espaces (comparaison de titres). */
    public String normalize(String text) {
        return jdbc.sql("SELECT btrim(regexp_replace(immutable_unaccent(lower(:t)), '[^a-z0-9]+', ' ', 'g'))")
                .param("t", text).query(String.class).single();
    }

    public boolean establishmentExists(UUID id) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM establishment WHERE id = :id)").param("id", id)
                .query(Boolean.class).single();
    }

    /** Intitulés des formations qui existent parmi {@code ids}. */
    public Map<UUID, String> programTitles(Collection<UUID> ids) {
        return titles("SELECT id, title FROM program WHERE id IN (:ids)", ids, null);
    }

    /** Parmi {@code ids}, les formations de cet établissement ou de ses composantes (à tous les niveaux). */
    public Map<UUID, String> programsInEstablishmentTree(UUID establishmentId, Collection<UUID> ids) {
        return titles("""
                SELECT id, title FROM program WHERE id IN (:ids) AND establishment_id IN
                  (WITH RECURSIVE d AS (SELECT id FROM establishment WHERE id = :etab
                                        UNION ALL SELECT c.id FROM establishment c JOIN d ON c.parent_id = d.id)
                   SELECT id FROM d)""", ids, establishmentId);
    }

    private Map<UUID, String> titles(String sql, Collection<UUID> ids, @Nullable UUID establishment) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        var spec = jdbc.sql(sql).param("ids", ids);
        if (establishment != null) {
            spec = spec.param("etab", establishment);
        }
        Map<UUID, String> found = new LinkedHashMap<>();
        spec.query((rs, n) -> found.put(rs.getObject("id", UUID.class), rs.getString("title"))).list();
        return found;
    }

    public List<UUID> existingReports(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT id FROM report WHERE id IN (:ids)").param("ids", ids).query(UUID.class).list();
    }

    public Optional<UUID> decisionEstablishment(UUID decisionId) {
        return jdbc.sql("SELECT establishment_id FROM decision WHERE id = :id").param("id", decisionId)
                .query(UUID.class).optional();
    }

    public UUID insert(DecisionInput in, String actor) {
        return bind(jdbc.sql("""
                        INSERT INTO decision (kind, establishment_id, decided_on, display_year, validity_years, wave, avis,
                                              report_id, previous_id, comment, entered_by)
                        VALUES (:kind, :establishment, :decidedOn, :year, :validity, :wave, :avis, :report, :previous, :comment, :actor)
                        RETURNING id"""), in).param("actor", actor).query(UUID.class).single();
    }

    public void update(UUID id, DecisionInput in) {
        bind(jdbc.sql("""
                UPDATE decision SET kind = :kind, establishment_id = :establishment, decided_on = :decidedOn, display_year = :year,
                                    validity_years = :validity, wave = :wave, avis = :avis, report_id = :report,
                                    previous_id = :previous, comment = :comment
                WHERE id = :id"""), in).param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec s, DecisionInput in) {
        return s.param("kind", in.kind()).param("establishment", in.establishmentId()).param("decidedOn", in.decidedOn())
                .param("year", in.displayYear()).param("validity", in.validityYears()).param("wave", in.wave())
                .param("avis", in.avis()).param("report", in.reportId()).param("previous", in.previousId())
                .param("comment", in.comment());
    }

    public void replacePrograms(UUID decisionId, List<ProgramAvisInput> programs) {
        jdbc.sql("DELETE FROM decision_program WHERE decision_id = :id").param("id", decisionId).update();
        for (ProgramAvisInput p : programs) {
            jdbc.sql("""
                            INSERT INTO decision_program (decision_id, program_id, avis, report_id, remark)
                            VALUES (:decision, :program, :avis, :report, :remark)""")
                    .param("decision", decisionId).param("program", p.programId()).param("avis", p.avis())
                    .param("report", p.reportId()).param("remark", p.remark()).update();
        }
    }

    /** Change le statut seulement si la décision a bien le statut de départ. Faux : introuvable ou autre statut. */
    public boolean transition(UUID id, String from, String to, @Nullable String validator) {
        return jdbc.sql("""
                        UPDATE decision SET status = :to, validated_by = coalesce(:validator, validated_by)
                        WHERE id = :id AND status = :from""")
                .param("id", id).param("from", from).param("to", to).param("validator", validator).update() == 1;
    }

    public boolean deleteDraft(UUID id) {
        return jdbc.sql("DELETE FROM decision WHERE id = :id AND status = 'BROUILLON'").param("id", id).update() == 1;
    }

    /** Étiquettes de cache du portail touchées par une décision : l'établissement et ses formations. */
    public List<String> cacheTags(UUID decisionId) {
        List<String> tags = new java.util.ArrayList<>(List.of("establishments", "programs"));
        jdbc.sql("""
                        SELECT 'establishment:' || e.slug FROM decision d JOIN establishment e ON e.id = d.establishment_id WHERE d.id = :id
                        UNION
                        SELECT 'program:' || p.slug FROM decision_program dp JOIN program p ON p.id = dp.program_id WHERE dp.decision_id = :id
                        UNION
                        SELECT 'establishment:' || e.slug FROM decision_program dp JOIN program p ON p.id = dp.program_id
                                JOIN establishment e ON e.id = p.establishment_id WHERE dp.decision_id = :id""")
                .param("id", decisionId).query(String.class).list().forEach(tags::add);
        return tags;
    }

    // -------------------------------------------------------------------- saisie rapide depuis une fiche

    /** Plus récente année d'une décision validée d'habilitation (établissement) ou d'accréditation (formation). */
    public Optional<Integer> latestValidYear(@Nullable UUID establishmentId, @Nullable UUID programId) {
        if (programId != null) {
            return jdbc.sql("""
                            SELECT max(d.display_year) FROM decision_program dp
                            JOIN decision d ON d.id = dp.decision_id AND d.status = 'VALIDE' WHERE dp.program_id = :id""")
                    .param("id", programId).query(Integer.class).optional();
        }
        return jdbc.sql("""
                        SELECT max(display_year) FROM decision
                        WHERE establishment_id = :id AND kind = 'HABILITATION' AND status = 'VALIDE' AND avis IS NOT NULL""")
                .param("id", establishmentId).query(Integer.class).optional();
    }

    /**
     * Décision validée créée en une fois depuis la fiche (avis et année, sans date de Conseil scientifique ni rapport).
     * Pour une formation, {@code programId} est renseigné et l'avis est porté par la formation.
     */
    public UUID insertValidatedQuick(String kind, UUID establishmentId, @Nullable UUID programId, String avis, int year,
                                     int validityYears, String actor) {
        UUID id = jdbc.sql("""
                        INSERT INTO decision (kind, establishment_id, display_year, validity_years, avis, comment, status,
                                              entered_by, validated_by, origin)
                        VALUES (:kind, :establishment, :year, :validity, :avis, 'Saisie rapide depuis la fiche', 'VALIDE', :actor, :actor,
                                'SAISIE_RAPIDE')
                        RETURNING id""")
                .param("kind", kind).param("establishment", establishmentId).param("year", year).param("validity", validityYears)
                .param("avis", programId == null ? avis : null).param("actor", actor).query(UUID.class).single();
        if (programId != null) {
            jdbc.sql("INSERT INTO decision_program (decision_id, program_id, avis) VALUES (:d, :p, :avis)")
                    .param("d", id).param("p", programId).param("avis", avis).update();
        }
        return id;
    }

    /** Vrai si l'établissement a une décision qui n'est pas une saisie rapide : elle protège la suppression. */
    public boolean establishmentHasKeptDecisions(UUID establishmentId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM decision WHERE establishment_id = :id AND origin <> 'SAISIE_RAPIDE')")
                .param("id", establishmentId).query(Boolean.class).single();
    }

    /** Vrai si la formation apparaît dans une décision qui n'est pas une saisie rapide. */
    public boolean programHasKeptDecisions(UUID programId) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM decision_program dp JOIN decision d ON d.id = dp.decision_id
                                       WHERE dp.program_id = :id AND d.origin <> 'SAISIE_RAPIDE')""")
                .param("id", programId).query(Boolean.class).single();
    }

    /** Supprime les décisions de saisie rapide d'un établissement (avant de supprimer sa fiche). */
    public void deleteQuickDecisionsOfEstablishment(UUID establishmentId) {
        jdbc.sql("DELETE FROM decision WHERE establishment_id = :id AND origin = 'SAISIE_RAPIDE'")
                .param("id", establishmentId).update();
    }

    /** Supprime les décisions de saisie rapide d'une formation (avant de supprimer sa fiche). */
    public void deleteQuickDecisionsOfProgram(UUID programId) {
        jdbc.sql("""
                        DELETE FROM decision WHERE origin = 'SAISIE_RAPIDE'
                          AND id IN (SELECT decision_id FROM decision_program WHERE program_id = :id)""")
                .param("id", programId).update();
    }

    private static DecisionRow decision(ResultSet rs) throws SQLException {
        return new DecisionRow(rs.getObject("id", UUID.class), rs.getString("kind"),
                rs.getObject("establishment_id", UUID.class), rs.getString("establishment_slug"),
                rs.getString("establishment_name"), rs.getObject("decided_on", LocalDate.class), rs.getInt("display_year"),
                rs.getInt("validity_years"), rs.getObject("valid_until", LocalDate.class), rs.getBoolean("estimated"),
                rs.getObject("wave", Integer.class), rs.getString("avis"), rs.getString("effective_avis"),
                rs.getObject("report_id", UUID.class), rs.getObject("previous_id", UUID.class), rs.getString("status"),
                rs.getString("comment"), rs.getString("entered_by"), rs.getString("validated_by"),
                rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));
    }
}
