package sn.anaqsup.portail.content.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Back-office : ressources documentaires, tous états confondus (brouillons compris). */
@Repository
public class AdminPublicationRepository {

    /** {@code coverGenerated} : la vignette est la première page du PDF actuel (et non une image choisie). */
    public record EditablePublication(UUID id, String slug, String title, @Nullable String summary, String kind,
                                      LocalDate publishedOn, boolean published, @Nullable UUID fileKey,
                                      @Nullable String fileName, @Nullable Integer fileSizeBytes, @Nullable String fileUrl,
                                      @Nullable UUID coverKey, boolean coverGenerated, @Nullable String titleEn,
                                      @Nullable String summaryEn, boolean enAuto, Instant updatedAt) {
    }

    /** Valeurs saisies (déjà contrôlées par le service). */
    public record PublicationChanges(String title, @Nullable String summary, String kind, LocalDate publishedOn,
                                     boolean published, @Nullable UUID fileKey, @Nullable UUID coverKey,
                                     @Nullable String titleEn, @Nullable String summaryEn, boolean enAuto) {
    }

    public record Slice(List<EditablePublication> rows, long total) {
    }

    private static final String SELECT = """
            SELECT pu.id, pu.slug, pu.title, pu.summary, pu.kind, pu.published_on, pu.status = 'PUBLIE' AS published,
                   pu.file_key, f.original_name AS file_name, f.size_bytes AS file_size_bytes, pu.file_url,
                   pu.cover_key, coalesce(c.derived_from = pu.file_key, false) AS cover_generated,
                   pu.title_en, pu.summary_en, pu.en_auto, pu.updated_at
            FROM publication pu
                     LEFT JOIN media f ON f.id = pu.file_key
                     LEFT JOIN media c ON c.id = pu.cover_key""";

    private final JdbcClient jdbc;

    AdminPublicationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Liste du back-office : les plus récemment modifiés d'abord ; recherche sur le titre (sans accents). */
    public Slice list(@Nullable String q, @Nullable String kind, int page, int size) {
        long[] total = {0};
        List<EditablePublication> rows = jdbc.sql("""
                        WITH p AS (SELECT immutable_unaccent(lower(btrim(CAST(:q AS text)))) AS q, CAST(:kind AS text) AS kind)
                        SELECT x.*, count(*) OVER () AS total FROM (%s) x CROSS JOIN p
                        WHERE (p.kind IS NULL OR x.kind = p.kind)
                          AND (p.q IS NULL OR p.q = ''
                               OR strpos(immutable_unaccent(lower(concat_ws(' ', x.title, x.title_en))), p.q) > 0)
                        ORDER BY x.updated_at DESC
                        LIMIT :size OFFSET :offset""".formatted(SELECT))
                .param("q", q).param("kind", kind).param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return publication(rs);
                })
                .list();
        return new Slice(rows, total[0]);
    }

    public Optional<EditablePublication> find(UUID id) {
        return jdbc.sql(SELECT + " WHERE pu.id = :id").param("id", id).query((rs, n) -> publication(rs)).optional();
    }

    public boolean slugExists(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM publication WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    public UUID insert(String slug, PublicationChanges changes, String author) {
        return bind(jdbc.sql("""
                        INSERT INTO publication (slug, title, summary, kind, published_on, file_key, cover_key,
                                                 title_en, summary_en, en_auto, status, author, published_at)
                        VALUES (:slug, :title, :summary, :kind, :publishedOn, :fileKey, :coverKey,
                                :titleEn, :summaryEn, :enAuto, CASE WHEN :published THEN 'PUBLIE' ELSE 'BROUILLON' END,
                                :author, CASE WHEN :published THEN now() END)
                        RETURNING id"""), changes)
                .param("slug", slug).param("author", author)
                .query(UUID.class).single();
    }

    public void update(UUID id, PublicationChanges changes, String author) {
        bind(jdbc.sql("""
                        UPDATE publication
                        SET title = :title, summary = :summary, kind = :kind, published_on = :publishedOn,
                            file_key = :fileKey, cover_key = :coverKey,
                            title_en = :titleEn, summary_en = :summaryEn, en_auto = :enAuto,
                            status = CASE WHEN :published THEN 'PUBLIE' ELSE 'BROUILLON' END,
                            published_at = CASE WHEN :published THEN coalesce(published_at, now()) ELSE published_at END,
                            author = :author, updated_at = now()
                        WHERE id = :id"""), changes)
                .param("id", id).param("author", author)
                .update();
    }

    public void delete(UUID id) {
        jdbc.sql("DELETE FROM publication WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, PublicationChanges changes) {
        return statement.param("title", changes.title()).param("summary", changes.summary()).param("kind", changes.kind())
                .param("publishedOn", changes.publishedOn()).param("published", changes.published())
                .param("fileKey", changes.fileKey()).param("coverKey", changes.coverKey())
                .param("titleEn", changes.titleEn()).param("summaryEn", changes.summaryEn()).param("enAuto", changes.enAuto());
    }

    private static EditablePublication publication(ResultSet rs) throws SQLException {
        return new EditablePublication(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("title"),
                rs.getString("summary"), rs.getString("kind"), rs.getObject("published_on", LocalDate.class),
                rs.getBoolean("published"), rs.getObject("file_key", UUID.class), rs.getString("file_name"),
                rs.getObject("file_size_bytes", Integer.class), rs.getString("file_url"), rs.getObject("cover_key", UUID.class),
                rs.getBoolean("cover_generated"), rs.getString("title_en"), rs.getString("summary_en"), rs.getBoolean("en_auto"),
                rs.getTimestamp("updated_at").toInstant());
    }
}
