package sn.anaqsup.portail.content.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Back-office : événements (brouillons compris). */
@Repository
public class AdminEventRepository {

    public record EditableEvent(UUID id, String slug, String title, @Nullable String summary, String bodyHtml,
                                @Nullable String location, Instant startsAt, @Nullable Instant endsAt, boolean published,
                                @Nullable String titleEn, @Nullable String summaryEn, @Nullable String bodyHtmlEn,
                                boolean enAuto, Instant updatedAt) {
    }

    public record EventChanges(String title, @Nullable String summary, String bodyHtml, @Nullable String location,
                               Instant startsAt, @Nullable Instant endsAt, boolean published, @Nullable String titleEn,
                               @Nullable String summaryEn, @Nullable String bodyHtmlEn, boolean enAuto) {
    }

    public record Slice(List<EditableEvent> rows, long total) {
    }

    private static final String SELECT = """
            SELECT e.id, e.slug, e.title, e.summary, e.body_html, e.location, e.starts_at, e.ends_at,
                   e.status = 'PUBLIE' AS published, e.title_en, e.summary_en, e.body_html_en, e.en_auto, e.updated_at
            FROM event e""";

    private final JdbcClient jdbc;

    AdminEventRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** À venir d'abord (le plus proche en tête), puis passés (le plus récent en tête). */
    public Slice list(@Nullable String q, @Nullable Boolean upcoming, int page, int size) {
        long[] total = {0};
        List<EditableEvent> rows = jdbc.sql("""
                        WITH p AS (SELECT immutable_unaccent(lower(btrim(CAST(:q AS text)))) AS q, CAST(:upcoming AS boolean) AS upcoming)
                        SELECT x.*, count(*) OVER () AS total FROM (%s) x CROSS JOIN p
                        WHERE (p.q IS NULL OR p.q = ''
                               OR strpos(immutable_unaccent(lower(concat_ws(' ', x.title, x.title_en, x.location))), p.q) > 0)
                          AND (p.upcoming IS NULL OR (coalesce(x.ends_at, x.starts_at) >= now()) = p.upcoming)
                        ORDER BY coalesce(x.ends_at, x.starts_at) >= now() DESC,
                                 CASE WHEN coalesce(x.ends_at, x.starts_at) >= now() THEN x.starts_at END ASC,
                                 x.starts_at DESC
                        LIMIT :size OFFSET :offset""".formatted(SELECT))
                .param("q", q).param("upcoming", upcoming).param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return event(rs);
                })
                .list();
        return new Slice(rows, total[0]);
    }

    public Optional<EditableEvent> find(UUID id) {
        return jdbc.sql(SELECT + " WHERE e.id = :id").param("id", id).query((rs, n) -> event(rs)).optional();
    }

    public boolean slugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM event WHERE slug = :slug)").param("slug", slug).query(Boolean.class).single();
    }

    public UUID insert(String slug, EventChanges changes, String author) {
        return bind(jdbc.sql("""
                        INSERT INTO event (slug, title, summary, body_html, location, starts_at, ends_at, status, author,
                                           published_at, title_en, summary_en, body_html_en, en_auto)
                        VALUES (:slug, :title, :summary, :body, :location, :startsAt, :endsAt,
                                CASE WHEN :published THEN 'PUBLIE' ELSE 'BROUILLON' END, :author,
                                CASE WHEN :published THEN now() END, :titleEn, :summaryEn, :bodyEn, :enAuto)
                        RETURNING id"""), changes)
                .param("slug", slug).param("author", author)
                .query(UUID.class).single();
    }

    public void update(UUID id, EventChanges changes, String author) {
        bind(jdbc.sql("""
                        UPDATE event
                        SET title = :title, summary = :summary, body_html = :body, location = :location,
                            starts_at = :startsAt, ends_at = :endsAt,
                            status = CASE WHEN :published THEN 'PUBLIE' ELSE 'BROUILLON' END,
                            published_at = CASE WHEN :published THEN coalesce(published_at, now()) ELSE published_at END,
                            title_en = :titleEn, summary_en = :summaryEn, body_html_en = :bodyEn, en_auto = :enAuto,
                            author = :author, updated_at = now()
                        WHERE id = :id"""), changes)
                .param("id", id).param("author", author)
                .update();
    }

    public void delete(UUID id) {
        jdbc.sql("DELETE FROM event WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, EventChanges c) {
        return statement.param("title", c.title()).param("summary", c.summary()).param("body", c.bodyHtml())
                .param("location", c.location()).param("startsAt", Timestamp.from(c.startsAt()))
                .param("endsAt", c.endsAt() == null ? null : Timestamp.from(c.endsAt()))
                .param("published", c.published()).param("titleEn", c.titleEn()).param("summaryEn", c.summaryEn())
                .param("bodyEn", c.bodyHtmlEn()).param("enAuto", c.enAuto());
    }

    private static EditableEvent event(ResultSet rs) throws SQLException {
        Timestamp endsAt = rs.getTimestamp("ends_at");
        return new EditableEvent(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("title"),
                rs.getString("summary"), rs.getString("body_html"), rs.getString("location"),
                rs.getTimestamp("starts_at").toInstant(), endsAt == null ? null : endsAt.toInstant(), rs.getBoolean("published"),
                rs.getString("title_en"), rs.getString("summary_en"), rs.getString("body_html_en"), rs.getBoolean("en_auto"),
                rs.getTimestamp("updated_at").toInstant());
    }
}
