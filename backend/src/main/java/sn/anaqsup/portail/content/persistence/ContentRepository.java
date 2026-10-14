package sn.anaqsup.portail.content.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lecture des contenus PUBLIÉS uniquement (statut PUBLIE et date de publication atteinte).
 *
 * <p>Langue : avec {@code en = true}, chaque contenu est servi en anglais s'il a une version anglaise,
 * sinon en français. Le choix se fait par contenu, jamais champ par champ (pas de titre anglais sur un
 * corps français) ; la colonne {@code lang} indique la langue réellement servie.
 */
@Repository
public class ContentRepository {

    public record Slice<T>(List<T> rows, long total) {
    }

    public record NewsRow(String slug, String lang, String title, @Nullable String summary, Instant publishedAt,
                          String categorySlug, String categoryLabel, @Nullable String coverImageKey, boolean featured,
                          @Nullable String bodyHtml, Instant updatedAt) {
    }

    /** Article voisin (précédent ou suivant dans l'ordre de publication) : de quoi faire un lien. */
    public record NewsLink(String slug, String lang, String title) {
    }

    public record CategoryRow(String slug, String label) {
    }

    public record EventRow(String slug, String lang, String title, @Nullable String summary, @Nullable String location,
                           Instant startsAt, @Nullable Instant endsAt, @Nullable String bodyHtml) {
    }

    /** Document : PDF téléversé ({@code fileKey}, taille et type lus dans la médiathèque) ou lien externe ({@code fileUrl}). */
    public record PublicationRow(String slug, String lang, String title, String kind, @Nullable String summary,
                                 LocalDate publishedOn, @Nullable String fileUrl, @Nullable UUID fileKey,
                                 @Nullable String fileContentType, @Nullable Integer fileSizeBytes, @Nullable UUID coverKey) {
    }

    /** Critères de la liste des documents ; {@code null} = pas de filtre. */
    public record PublicationFilter(@Nullable String q, @Nullable String kind, @Nullable Integer year) {
    }

    public record PageRow(String slug, String lang, String title, @Nullable String summary, String bodyHtml,
                          @Nullable String imageKey, @Nullable String imageName, @Nullable String imageRole,
                          Instant updatedAt) {
    }

    public record PartnerRow(String name, @Nullable String url, @Nullable String logoKey) {
    }

    /** Contenu visible du public : publié ET date de publication atteinte (publication programmée). */
    private static String published(String alias) {
        return "%1$s.status = 'PUBLIE' AND %1$s.published_at <= now()".formatted(alias);
    }

    /**
     * Jointure qui calcule une fois par ligne si la version anglaise est servie ({@code tr.en}).
     * La version anglaise est complète dès que le titre existe (contraintes {@code *_en_complete_chk}).
     */
    private static String translation(String alias) {
        return "CROSS JOIN LATERAL (SELECT CAST(:en AS boolean) AND %1$s.title_en IS NOT NULL AS en) tr".formatted(alias);
    }

    /** Colonne traduisible : version anglaise si elle est servie, française sinon. */
    private static String text(String alias, String column) {
        return "CASE WHEN tr.en THEN %1$s.%2$s_en ELSE %1$s.%2$s END AS %2$s".formatted(alias, column);
    }

    private static final String LANG = "CASE WHEN tr.en THEN 'en' ELSE 'fr' END AS lang";

    /** Le libellé d'une catégorie se traduit indépendamment des articles (repli sur le français). */
    private static final String CATEGORY_LABEL =
            "CASE WHEN CAST(:en AS boolean) THEN coalesce(c.label_en, c.label) ELSE c.label END AS category_label";

    private final JdbcClient jdbc;

    ContentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------------------------- actualités

    public Slice<NewsRow> news(@Nullable String categorySlug, boolean en, int page, int size) {
        long[] total = {0};
        List<NewsRow> rows = jdbc.sql("""
                        WITH p AS (SELECT CAST(:category AS text) AS category)
                        SELECT n.slug, %s, %s, %s, n.published_at, c.slug AS category_slug, %s,
                               n.cover_image_key, n.featured, NULL AS body_html, n.updated_at, count(*) OVER () AS total
                        FROM news_item n
                                 JOIN news_category c ON c.id = n.category_id
                                 CROSS JOIN p
                                 %s
                        WHERE %s AND (p.category IS NULL OR c.slug = p.category)
                        ORDER BY n.featured DESC, n.published_at DESC
                        LIMIT :size OFFSET :offset""".formatted(LANG, text("n", "title"), text("n", "summary"),
                        CATEGORY_LABEL, translation("n"), published("n")))
                .param("category", categorySlug).param("en", en).param("size", size).param("offset", (long) page * size)
                .query((rs, rowNum) -> {
                    total[0] = rs.getLong("total");
                    return news(rs);
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<NewsRow> newsItem(String slug, boolean en) {
        return jdbc.sql("""
                        SELECT n.slug, %s, %s, %s, n.published_at, c.slug AS category_slug, %s,
                               n.cover_image_key, n.featured, %s, n.updated_at
                        FROM news_item n JOIN news_category c ON c.id = n.category_id %s
                        WHERE n.slug = :slug AND %s""".formatted(LANG, text("n", "title"), text("n", "summary"),
                        CATEGORY_LABEL, text("n", "body_html"), translation("n"), published("n")))
                .param("slug", slug).param("en", en)
                .query((rs, rowNum) -> news(rs))
                .optional();
    }

    /**
     * Articles publiés juste avant ({@code older = true}) ou juste après un article ; à date égale, l'adresse
     * départage (ordre stable, sans article sauté ni répété).
     */
    public Optional<NewsLink> neighbour(String slug, Instant publishedAt, boolean older, boolean en) {
        // Comparaison et tri fixes (aucune entrée utilisateur interpolée).
        String comparison = older ? "<" : ">";
        String order = older ? "DESC" : "ASC";
        return jdbc.sql("""
                        SELECT n.slug, %s, %s
                        FROM news_item n %s
                        WHERE %s AND (n.published_at, n.slug) %s (CAST(:at AS timestamptz), CAST(:slug AS text))
                        ORDER BY n.published_at %s, n.slug %s
                        LIMIT 1""".formatted(LANG, text("n", "title"), translation("n"), published("n"), comparison, order, order))
                .param("at", java.sql.Timestamp.from(publishedAt)).param("slug", slug).param("en", en)
                .query((rs, n) -> new NewsLink(rs.getString("slug"), rs.getString("lang"), rs.getString("title")))
                .optional();
    }

    public List<CategoryRow> categories(boolean en) {
        return jdbc.sql("""
                        SELECT c.slug, %s FROM news_category c
                        ORDER BY c.position, c.label""".formatted(CATEGORY_LABEL))
                .param("en", en)
                .query((rs, rowNum) -> new CategoryRow(rs.getString("slug"), rs.getString("category_label")))
                .list();
    }

    // ------------------------------------------------------------------------------------- événements

    public Slice<EventRow> events(boolean upcoming, boolean en, int page, int size) {
        // Opérateur et tri fixes (aucune entrée utilisateur interpolée).
        String comparison = upcoming ? ">=" : "<";
        String order = upcoming ? "ASC" : "DESC";
        long[] total = {0};
        List<EventRow> rows = jdbc.sql("""
                        SELECT e.slug, %s, %s, %s, e.location, e.starts_at, e.ends_at, NULL AS body_html,
                               count(*) OVER () AS total
                        FROM event e %s
                        WHERE %s AND coalesce(e.ends_at, e.starts_at) %s now()
                        ORDER BY e.starts_at %s
                        LIMIT :size OFFSET :offset""".formatted(LANG, text("e", "title"), text("e", "summary"),
                        translation("e"), published("e"), comparison, order))
                .param("en", en).param("size", size).param("offset", (long) page * size)
                .query((rs, rowNum) -> {
                    total[0] = rs.getLong("total");
                    return event(rs);
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    public Optional<EventRow> event(String slug, boolean en) {
        return jdbc.sql("""
                        SELECT e.slug, %s, %s, %s, e.location, e.starts_at, e.ends_at, %s
                        FROM event e %s WHERE e.slug = :slug AND %s""".formatted(LANG, text("e", "title"),
                        text("e", "summary"), text("e", "body_html"), translation("e"), published("e")))
                .param("slug", slug).param("en", en)
                .query((rs, rowNum) -> event(rs))
                .optional();
    }

    // ----------------------------------------------------------------------------------- publications

    /**
     * Documents publiés, du plus récent au plus ancien. Recherche : chaque mot doit apparaître dans le titre
     * ou le résumé, français ou anglais, sans tenir compte des accents ni des majuscules (« referentiel »
     * trouve « Référentiel »). Quelques centaines de documents au plus : pas d'index plein texte nécessaire.
     */
    public Slice<PublicationRow> publications(PublicationFilter filter, boolean en, int page, int size) {
        long[] total = {0};
        List<PublicationRow> rows = jdbc.sql("""
                        WITH p AS (SELECT CAST(:kind AS text) AS kind, CAST(:year AS integer) AS year,
                                          immutable_unaccent(lower(btrim(CAST(:q AS text)))) AS q)
                        SELECT pu.slug, %s, %s, pu.kind, %s, pu.published_on, pu.file_url, pu.file_key,
                               f.content_type AS file_content_type, f.size_bytes AS file_size_bytes, pu.cover_key,
                               count(*) OVER () AS total
                        FROM publication pu
                                 CROSS JOIN p
                                 LEFT JOIN media f ON f.id = pu.file_key
                                 %s
                        WHERE %s
                          AND (p.kind IS NULL OR pu.kind = p.kind)
                          AND (p.year IS NULL OR extract(year FROM pu.published_on) = p.year)
                          AND (p.q IS NULL OR p.q = '' OR NOT EXISTS (
                                SELECT 1 FROM regexp_split_to_table(p.q, '\\s+') AS word
                                WHERE word <> '' AND strpos(immutable_unaccent(lower(concat_ws(' ',
                                      pu.title, pu.summary, pu.title_en, pu.summary_en))), word) = 0))
                        ORDER BY pu.published_on DESC, pu.title
                        LIMIT :size OFFSET :offset""".formatted(LANG, text("pu", "title"), text("pu", "summary"),
                        translation("pu"), published("pu")))
                .param("kind", filter.kind()).param("year", filter.year()).param("q", filter.q())
                .param("en", en).param("size", size).param("offset", (long) page * size)
                .query((rs, rowNum) -> {
                    total[0] = rs.getLong("total");
                    return new PublicationRow(rs.getString("slug"), rs.getString("lang"), rs.getString("title"), rs.getString("kind"),
                            rs.getString("summary"), rs.getObject("published_on", LocalDate.class), rs.getString("file_url"),
                            rs.getObject("file_key", UUID.class), rs.getString("file_content_type"),
                            rs.getObject("file_size_bytes", Integer.class), rs.getObject("cover_key", UUID.class));
                })
                .list();
        return new Slice<>(rows, total[0]);
    }

    /** Années pour lesquelles au moins un document (du type demandé) est publié, la plus récente d'abord. */
    public List<Integer> publicationYears(@Nullable String kind) {
        return jdbc.sql("""
                        SELECT DISTINCT CAST(extract(year FROM pu.published_on) AS integer) AS year
                        FROM publication pu
                        WHERE %s AND (CAST(:kind AS text) IS NULL OR pu.kind = :kind)
                        ORDER BY year DESC""".formatted(published("pu")))
                .param("kind", kind)
                .query(Integer.class)
                .list();
    }

    // ---------------------------------------------------------------------------------------- pages

    public Optional<PageRow> page(String slug, boolean en) {
        return jdbc.sql("""
                        SELECT ip.slug, %s, %s, %s, %s, ip.image_key, ip.image_name,
                               -- Fonction : version anglaise si elle existe, sinon française (le nom ne se traduit pas).
                               CASE WHEN CAST(:en AS boolean) THEN coalesce(ip.image_role_en, ip.image_role)
                                    ELSE ip.image_role END AS image_role,
                               ip.updated_at
                        FROM institutional_page ip %s WHERE ip.slug = :slug AND %s""".formatted(LANG, text("ip", "title"),
                        text("ip", "summary"), text("ip", "body_html"), translation("ip"), published("ip")))
                .param("slug", slug).param("en", en)
                .query((rs, rowNum) -> new PageRow(rs.getString("slug"), rs.getString("lang"), rs.getString("title"),
                        rs.getString("summary"), rs.getString("body_html"), rs.getString("image_key"),
                        rs.getString("image_name"), rs.getString("image_role"), instant(rs, "updated_at")))
                .optional();
    }

    // ------------------------------------------------------------------------------------ partenaires

    public List<PartnerRow> partners() {
        return jdbc.sql("SELECT name, url, logo_key FROM partner WHERE enabled ORDER BY position, name")
                .query((rs, rowNum) -> new PartnerRow(rs.getString("name"), rs.getString("url"), rs.getString("logo_key")))
                .list();
    }

    // ------------------------------------------------------------------------------------- mapping

    private static NewsRow news(ResultSet rs) throws SQLException {
        return new NewsRow(rs.getString("slug"), rs.getString("lang"), rs.getString("title"), rs.getString("summary"),
                instant(rs, "published_at"), rs.getString("category_slug"), rs.getString("category_label"),
                rs.getString("cover_image_key"), rs.getBoolean("featured"), rs.getString("body_html"),
                instant(rs, "updated_at"));
    }

    private static EventRow event(ResultSet rs) throws SQLException {
        return new EventRow(rs.getString("slug"), rs.getString("lang"), rs.getString("title"), rs.getString("summary"),
                rs.getString("location"), instant(rs, "starts_at"), instant(rs, "ends_at"), rs.getString("body_html"));
    }

    private static @Nullable Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
