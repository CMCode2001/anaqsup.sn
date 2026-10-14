package sn.anaqsup.portail.content.persistence;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Mentions « J'aime » / « Je n'aime pas » des lecteurs : un vote par navigateur et par actualité.
 * {@code visitor} est l'empreinte SHA-256 (32 octets) de l'identifiant anonyme du navigateur.
 */
@Repository
public class NewsReactionRepository {

    public enum Reaction {
        LIKE(1), DISLIKE(-1);

        private final int value;

        Reaction(int value) {
            this.value = value;
        }

        static Reaction of(int value) {
            return Arrays.stream(values()).filter(r -> r.value == value).findFirst().orElseThrow();
        }
    }

    private final JdbcClient jdbc;

    NewsReactionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Actualité visible du public (publiée, date atteinte) : on ne vote que sur celles-là. */
    public Optional<UUID> publishedNewsId(String slug) {
        return jdbc.sql("SELECT id FROM news_item WHERE slug = :slug AND status = 'PUBLIE' AND published_at <= now()")
                .param("slug", slug).query(UUID.class).optional();
    }

    public long likes(UUID newsId) {
        return jdbc.sql("SELECT count(*) FROM news_reaction WHERE news_id = :id AND value = 1")
                .param("id", newsId).query(Long.class).single();
    }

    public Optional<Reaction> mine(UUID newsId, byte[] visitor) {
        return jdbc.sql("SELECT value FROM news_reaction WHERE news_id = :id AND visitor = :visitor")
                .param("id", newsId).param("visitor", visitor).query(Integer.class).optional().map(Reaction::of);
    }

    /** Vote, ou changement d'avis (un seul vote par navigateur). */
    public void set(UUID newsId, byte[] visitor, Reaction reaction) {
        jdbc.sql("""
                        INSERT INTO news_reaction (news_id, visitor, value) VALUES (:id, :visitor, :value)
                        ON CONFLICT (news_id, visitor) DO UPDATE SET value = EXCLUDED.value, updated_at = now()
                        WHERE news_reaction.value <> EXCLUDED.value""")
                .param("id", newsId).param("visitor", visitor).param("value", reaction.value).update();
    }

    /** Vote annulé. */
    public void clear(UUID newsId, byte[] visitor) {
        jdbc.sql("DELETE FROM news_reaction WHERE news_id = :id AND visitor = :visitor")
                .param("id", newsId).param("visitor", visitor).update();
    }
}
