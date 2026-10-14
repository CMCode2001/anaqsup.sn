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

/** Back-office : actualités (brouillons compris) et rubriques. */
@Repository
public class AdminNewsRepository {

    public record EditableNews(UUID id, String slug, String title, @Nullable String summary, String bodyHtml,
                               String categorySlug, String categoryLabel, @Nullable String coverKey, boolean published,
                               @Nullable Instant publishedAt, boolean featured, @Nullable String titleEn,
                               @Nullable String summaryEn, @Nullable String bodyHtmlEn, boolean enAuto, boolean legacy,
                               Instant updatedAt, long likes, long dislikes) {
    }

    /** Valeurs saisies, déjà contrôlées par le service. */
    public record NewsChanges(String title, @Nullable String summary, String bodyHtml, UUID categoryId,
                              @Nullable String coverKey, boolean published, Instant publishedAt, boolean featured,
                              @Nullable String titleEn, @Nullable String summaryEn, @Nullable String bodyHtmlEn,
                              boolean enAuto) {
    }

    public record Slice(List<EditableNews> rows, long total) {
    }

    public record CategoryRow(UUID id, String slug, String label, @Nullable String labelEn, int position, long articles) {
    }

    private static final String SELECT = """
            SELECT n.id, n.slug, n.title, n.summary, n.body_html, c.slug AS category_slug, c.label AS category_label,
                   n.cover_image_key, n.status = 'PUBLIE' AS published, n.published_at, n.featured, n.title_en,
                   n.summary_en, n.body_html_en, n.en_auto, n.legacy_wordpress_id IS NOT NULL AS legacy, n.updated_at,
                   (SELECT count(*) FROM news_reaction r WHERE r.news_id = n.id AND r.value = 1) AS likes,
                   (SELECT count(*) FROM news_reaction r WHERE r.news_id = n.id AND r.value = -1) AS dislikes
            FROM news_item n JOIN news_category c ON c.id = n.category_id""";

    private final JdbcClient jdbc;

    AdminNewsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // -------------------------------------------------------------------------------------- articles

    /** Liste : les plus récents d'abord (brouillons sans date en tête) ; recherche dans le titre, sans accents. */
    public Slice list(@Nullable String q, @Nullable String category, int page, int size) {
        long[] total = {0};
        List<EditableNews> rows = jdbc.sql("""
                        WITH p AS (SELECT immutable_unaccent(lower(btrim(CAST(:q AS text)))) AS q, CAST(:category AS text) AS category)
                        SELECT x.*, count(*) OVER () AS total FROM (%s) x CROSS JOIN p
                        WHERE (p.category IS NULL OR x.category_slug = p.category)
                          AND (p.q IS NULL OR p.q = ''
                               OR strpos(immutable_unaccent(lower(concat_ws(' ', x.title, x.title_en))), p.q) > 0)
                        ORDER BY x.featured DESC, x.published_at DESC NULLS FIRST, x.updated_at DESC
                        LIMIT :size OFFSET :offset""".formatted(SELECT))
                .param("q", q).param("category", category).param("size", size).param("offset", (long) page * size)
                .query((rs, n) -> {
                    total[0] = rs.getLong("total");
                    return news(rs);
                })
                .list();
        return new Slice(rows, total[0]);
    }

    public Optional<EditableNews> find(UUID id) {
        return jdbc.sql(SELECT + " WHERE n.id = :id").param("id", id).query((rs, n) -> news(rs)).optional();
    }

    public boolean slugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM news_item WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    /** Un seul article « À la une » (index unique) : l'ancien est désépinglé dans la même transaction. */
    public void unpinAllExcept(@Nullable UUID id) {
        jdbc.sql("UPDATE news_item SET featured = false WHERE featured AND id IS DISTINCT FROM :id").param("id", id).update();
    }

    public UUID insert(String slug, NewsChanges changes, String author) {
        return bind(jdbc.sql("""
                        INSERT INTO news_item (slug, category_id, title, summary, body_html, cover_image_key, status, author,
                                               published_at, featured, title_en, summary_en, body_html_en, en_auto)
                        VALUES (:slug, :category, :title, :summary, :body, :cover,
                                CASE WHEN :published THEN 'PUBLIE' ELSE 'BROUILLON' END, :author, :publishedAt, :featured,
                                :titleEn, :summaryEn, :bodyEn, :enAuto)
                        RETURNING id"""), changes)
                .param("slug", slug).param("author", author)
                .query(UUID.class).single();
    }

    public void update(UUID id, NewsChanges changes, String author) {
        bind(jdbc.sql("""
                        UPDATE news_item
                        SET category_id = :category, title = :title, summary = :summary, body_html = :body,
                            cover_image_key = :cover, status = CASE WHEN :published THEN 'PUBLIE' ELSE 'BROUILLON' END,
                            published_at = :publishedAt, featured = :featured, title_en = :titleEn,
                            summary_en = :summaryEn, body_html_en = :bodyEn, en_auto = :enAuto,
                            author = :author, updated_at = now()
                        WHERE id = :id"""), changes)
                .param("id", id).param("author", author)
                .update();
    }

    public void delete(UUID id) {
        jdbc.sql("DELETE FROM news_item WHERE id = :id").param("id", id).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, NewsChanges c) {
        return statement.param("category", c.categoryId()).param("title", c.title()).param("summary", c.summary())
                .param("body", c.bodyHtml()).param("cover", c.coverKey()).param("published", c.published())
                .param("publishedAt", Timestamp.from(c.publishedAt())).param("featured", c.featured())
                .param("titleEn", c.titleEn()).param("summaryEn", c.summaryEn()).param("bodyEn", c.bodyHtmlEn())
                .param("enAuto", c.enAuto());
    }

    private static EditableNews news(ResultSet rs) throws SQLException {
        Timestamp publishedAt = rs.getTimestamp("published_at");
        return new EditableNews(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("title"),
                rs.getString("summary"), rs.getString("body_html"), rs.getString("category_slug"),
                rs.getString("category_label"), rs.getString("cover_image_key"), rs.getBoolean("published"),
                publishedAt == null ? null : publishedAt.toInstant(), rs.getBoolean("featured"), rs.getString("title_en"),
                rs.getString("summary_en"), rs.getString("body_html_en"), rs.getBoolean("en_auto"), rs.getBoolean("legacy"),
                rs.getTimestamp("updated_at").toInstant(), rs.getLong("likes"), rs.getLong("dislikes"));
    }

    // -------------------------------------------------------------------------------------- rubriques

    public List<CategoryRow> categories() {
        return jdbc.sql("""
                        SELECT c.id, c.slug, c.label, c.label_en, c.position,
                               (SELECT count(*) FROM news_item n WHERE n.category_id = c.id) AS articles
                        FROM news_category c ORDER BY c.position, c.label""")
                .query((rs, n) -> new CategoryRow(rs.getObject("id", UUID.class), rs.getString("slug"), rs.getString("label"),
                        rs.getString("label_en"), rs.getInt("position"), rs.getLong("articles")))
                .list();
    }

    public Optional<CategoryRow> category(UUID id) {
        return categories().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    public Optional<UUID> categoryId(String slug) {
        return jdbc.sql("SELECT id FROM news_category WHERE slug = :slug").param("slug", slug).query(UUID.class).optional();
    }

    public boolean categorySlugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM news_category WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    public UUID insertCategory(String slug, String label, @Nullable String labelEn) {
        return jdbc.sql("""
                        INSERT INTO news_category (slug, label, label_en, position)
                        VALUES (:slug, :label, :labelEn, (SELECT coalesce(max(position), 0) + 1 FROM news_category))
                        RETURNING id""")
                .param("slug", slug).param("label", label).param("labelEn", labelEn)
                .query(UUID.class).single();
    }

    public void updateCategory(UUID id, String label, @Nullable String labelEn) {
        jdbc.sql("UPDATE news_category SET label = :label, label_en = :labelEn WHERE id = :id")
                .param("id", id).param("label", label).param("labelEn", labelEn).update();
    }

    public void reorderCategories(List<UUID> ids) {
        for (int i = 0; i < ids.size(); i++) {
            jdbc.sql("UPDATE news_category SET position = :position WHERE id = :id")
                    .param("position", i + 1).param("id", ids.get(i)).update();
        }
    }

    public void deleteCategory(UUID id) {
        jdbc.sql("DELETE FROM news_category WHERE id = :id").param("id", id).update();
    }
}
