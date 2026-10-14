package sn.anaqsup.portail.content.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lectures et écritures du back-office : pages institutionnelles et partenaires (tous états confondus). */
@Repository
public class AdminContentRepository {

    public record EditablePage(String slug, String title, @Nullable String summary, String bodyHtml,
                               @Nullable String titleEn, @Nullable String summaryEn, @Nullable String bodyHtmlEn,
                               @Nullable String imageKey, @Nullable String imageName, @Nullable String imageRole,
                               @Nullable String imageRoleEn, boolean enAuto, @Nullable Instant updatedAt) {
    }

    public record PartnerRow(UUID id, String name, @Nullable String url, @Nullable String logoKey, int position,
                             boolean enabled) {
    }

    private final JdbcClient jdbc;

    AdminContentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------------------------------ pages

    public List<EditablePage> pages() {
        return jdbc.sql(PAGE_SELECT + " ORDER BY slug").query((rs, n) -> page(rs)).list();
    }

    public Optional<EditablePage> page(String slug) {
        return jdbc.sql(PAGE_SELECT + " WHERE slug = :slug").param("slug", slug).query((rs, n) -> page(rs)).optional();
    }

    public void updatePage(EditablePage page, String author) {
        jdbc.sql("""
                        UPDATE institutional_page
                        SET title = :title, summary = :summary, body_html = :body,
                            title_en = :titleEn, summary_en = :summaryEn, body_html_en = :bodyEn,
                            image_key = :imageKey, image_name = :imageName, image_role = :imageRole,
                            image_role_en = :imageRoleEn,
                            en_auto = :enAuto, author = :author, updated_at = now()
                        WHERE slug = :slug""")
                .param("slug", page.slug()).param("title", page.title()).param("summary", page.summary())
                .param("body", page.bodyHtml()).param("titleEn", page.titleEn()).param("summaryEn", page.summaryEn())
                .param("bodyEn", page.bodyHtmlEn()).param("imageKey", page.imageKey())
                .param("imageName", page.imageName()).param("imageRole", page.imageRole())
                .param("imageRoleEn", page.imageRoleEn())
                .param("enAuto", page.enAuto()).param("author", author)
                .update();
    }

    private static final String PAGE_SELECT = """
            SELECT slug, title, summary, body_html, title_en, summary_en, body_html_en,
                   image_key, image_name, image_role, image_role_en, en_auto, updated_at
            FROM institutional_page""";

    private static EditablePage page(ResultSet rs) throws SQLException {
        return new EditablePage(rs.getString("slug"), rs.getString("title"), rs.getString("summary"),
                rs.getString("body_html"), rs.getString("title_en"), rs.getString("summary_en"),
                rs.getString("body_html_en"), rs.getString("image_key"), rs.getString("image_name"),
                rs.getString("image_role"), rs.getString("image_role_en"), rs.getBoolean("en_auto"),
                rs.getTimestamp("updated_at").toInstant());
    }

    // ------------------------------------------------------------------------------------- partenaires

    public List<PartnerRow> partners() {
        return jdbc.sql("SELECT id, name, url, logo_key, position, enabled FROM partner ORDER BY position, name")
                .query((rs, n) -> partner(rs)).list();
    }

    public Optional<PartnerRow> partner(UUID id) {
        return jdbc.sql("SELECT id, name, url, logo_key, position, enabled FROM partner WHERE id = :id")
                .param("id", id).query((rs, n) -> partner(rs)).optional();
    }

    public UUID insertPartner(String name, @Nullable String url, @Nullable String logoKey, boolean enabled) {
        return jdbc.sql("""
                        INSERT INTO partner (name, url, logo_key, enabled, position)
                        VALUES (btrim(:name), :url, :logo, :enabled, (SELECT coalesce(max(position), 0) + 1 FROM partner))
                        RETURNING id""")
                .param("name", name).param("url", url).param("logo", logoKey).param("enabled", enabled)
                .query(UUID.class).single();
    }

    public void updatePartner(UUID id, String name, @Nullable String url, @Nullable String logoKey, boolean enabled) {
        jdbc.sql("""
                        UPDATE partner SET name = btrim(:name), url = :url, logo_key = :logo, enabled = :enabled,
                                           updated_at = now()
                        WHERE id = :id""")
                .param("id", id).param("name", name).param("url", url).param("logo", logoKey).param("enabled", enabled)
                .update();
    }

    public void deletePartner(UUID id) {
        jdbc.sql("DELETE FROM partner WHERE id = :id").param("id", id).update();
    }

    /** Nouvel ordre d'affichage : position = rang dans la liste reçue. */
    public void reorderPartners(List<UUID> ids) {
        for (int i = 0; i < ids.size(); i++) {
            jdbc.sql("UPDATE partner SET position = :position, updated_at = now() WHERE id = :id")
                    .param("position", i + 1).param("id", ids.get(i)).update();
        }
    }

    private static PartnerRow partner(ResultSet rs) throws SQLException {
        return new PartnerRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("url"),
                rs.getString("logo_key"), rs.getInt("position"), rs.getBoolean("enabled"));
    }
}
