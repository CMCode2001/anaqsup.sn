package sn.anaqsup.portail.content.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Écritures de la reprise WordPress : contenus publiés directement, au nom de « reprise.wordpress ». */
@Repository
public class LegacyImportRepository {

    private final JdbcClient jdbc;

    LegacyImportRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean hasArticle(long wordpressId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM news_item WHERE legacy_wordpress_id = :id)").param("id", wordpressId)
                .query(Boolean.class).single();
    }

    public boolean articleSlugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM news_item WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    public Optional<UUID> category(String slug) {
        return jdbc.sql("SELECT id FROM news_category WHERE slug = :slug").param("slug", slug).query(UUID.class).optional();
    }

    public void insertArticle(long wordpressId, String slug, UUID categoryId, String title, @Nullable String summary,
                              String bodyHtml, @Nullable UUID coverKey, Instant publishedAt, String author) {
        jdbc.sql("""
                        INSERT INTO news_item (slug, category_id, title, summary, body_html, cover_image_key, status, author,
                                               published_at, legacy_wordpress_id, created_at, updated_at)
                        VALUES (:slug, :category, :title, :summary, :body, :cover, 'PUBLIE', :author,
                                :publishedAt, :wordpressId, :publishedAt, :publishedAt)""")
                .param("slug", slug).param("category", categoryId).param("title", title).param("summary", summary)
                .param("body", bodyHtml).param("cover", coverKey == null ? null : coverKey.toString())
                .param("author", author).param("publishedAt", java.sql.Timestamp.from(publishedAt))
                .param("wordpressId", wordpressId)
                .update();
    }

    public boolean hasDocument(String legacyUrl) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM publication WHERE legacy_url = :url)").param("url", legacyUrl)
                .query(Boolean.class).single();
    }

    public boolean documentSlugTaken(String slug) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM publication WHERE slug = :slug)").param("slug", slug)
                .query(Boolean.class).single();
    }

    public void insertDocument(String legacyUrl, String slug, String title, String kind, LocalDate publishedOn,
                               UUID fileKey, @Nullable UUID coverKey, String author) {
        jdbc.sql("""
                        INSERT INTO publication (slug, title, kind, published_on, file_key, cover_key, status, author,
                                                 published_at, legacy_url)
                        VALUES (:slug, :title, :kind, :publishedOn, :fileKey, :coverKey, 'PUBLIE', :author, now(), :url)""")
                .param("slug", slug).param("title", title).param("kind", kind).param("publishedOn", publishedOn)
                .param("fileKey", fileKey).param("coverKey", coverKey).param("author", author).param("url", legacyUrl)
                .update();
    }
}
