package sn.anaqsup.portail.content.persistence;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Contenus à traduire automatiquement : sans version anglaise, ou dont la traduction automatique ne
 * correspond plus au texte français (empreinte {@code en_source} différente). Une traduction relue ou
 * corrigée dans le back-office ({@code en_auto = false}) n'est jamais proposée.
 */
@Repository
public class TranslationRepository {

    /** Tables traduisibles ; {@code tags} : étiquettes de cache du portail à vider après traduction. */
    public enum Type {
        NEWS("news_item", true, false),
        EVENT("event", true, false),
        PUBLICATION("publication", false, false),
        PAGE("institutional_page", true, true);

        private final String table;
        private final boolean body;
        /** Fonction de la personne en photo (le nom, nom propre, ne se traduit pas). */
        private final boolean role;

        Type(String table, boolean body, boolean role) {
            this.table = table;
            this.body = body;
            this.role = role;
        }

        public List<String> tags(String slug) {
            return switch (this) {
                case NEWS -> List.of("news", "news:" + slug);
                case EVENT -> List.of("events", "event:" + slug);
                case PUBLICATION -> List.of("publications");
                case PAGE -> List.of("page:" + slug);
            };
        }

        /** Empreinte du texte français : change dès qu'un champ traduit change. */
        private String fingerprint() {
            return "md5(concat_ws(E'\\x1f', t.title, coalesce(t.summary, ''), %s, %s))".formatted(
                    body ? "t.body_html" : "''", role ? "coalesce(t.image_role, '')" : "''");
        }
    }

    public record Pending(Type type, UUID id, String slug, String fingerprint, String title, @Nullable String summary,
                          @Nullable String bodyHtml, @Nullable String role) {
    }

    public record English(String title, @Nullable String summary, @Nullable String bodyHtml, @Nullable String role) {
    }

    private final JdbcClient jdbc;

    TranslationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Contenus publiés d'abord (visibles sur le site), puis brouillons ; les plus récents d'abord.
     * Actualités : seulement celles des {@code newsMaxAgeMonths} derniers mois (décision ANAQ-Sup du 14/10/2026 :
     * les archives reprises de l'ancien site restent en français, le quota DeepL ne les couvrirait pas).
     */
    public List<Pending> pending(Type type, int limit, int newsMaxAgeMonths) {
        return jdbc.sql("""
                        SELECT t.id, t.slug, t.title, t.summary, %s AS body_html, %s AS role, %s AS fingerprint
                        FROM %s t
                        WHERE t.status <> 'ARCHIVE'
                          AND (t.title_en IS NULL OR (t.en_auto AND t.en_source IS DISTINCT FROM %s))
                          %s
                        ORDER BY t.status = 'PUBLIE' DESC, t.updated_at DESC
                        LIMIT :limit""".formatted(type.body ? "t.body_html" : "NULL", type.role ? "t.image_role" : "NULL",
                        type.fingerprint(), type.table, type.fingerprint(),
                        type == Type.NEWS ? "AND (t.published_at IS NULL OR t.published_at >= now() - make_interval(months => :months))" : ""))
                .param("limit", limit).param("months", newsMaxAgeMonths)
                .query((rs, n) -> new Pending(type, rs.getObject("id", UUID.class), rs.getString("slug"),
                        rs.getString("fingerprint"), rs.getString("title"), rs.getString("summary"),
                        rs.getString("body_html"), rs.getString("role")))
                .list();
    }

    /**
     * Enregistre la traduction, sauf si le contenu a changé pendant ce temps (texte français modifié, ou
     * version anglaise saisie par un éditeur) : la traduction sera alors refaite au passage suivant.
     * La date de mise à jour n'est pas touchée : une traduction n'est pas une modification éditoriale.
     */
    public boolean save(Pending item, English english) {
        Type type = item.type();
        String columns = "title_en = :title, summary_en = :summary"
                + (type.body ? ", body_html_en = :body" : "")
                + (type.role ? ", image_role_en = coalesce(:role, image_role_en)" : "");
        return jdbc.sql("""
                        UPDATE %s t SET %s, en_auto = true, en_source = :fingerprint
                        WHERE t.id = :id AND %s = :fingerprint AND (t.title_en IS NULL OR t.en_auto)"""
                        .formatted(type.table, columns, type.fingerprint()))
                .param("id", item.id()).param("fingerprint", item.fingerprint())
                .param("title", english.title()).param("summary", english.summary())
                .param("body", english.bodyHtml()).param("role", english.role())
                .update() == 1;
    }
}
