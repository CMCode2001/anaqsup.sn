package sn.anaqsup.portail.content.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;

import jakarta.validation.constraints.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.content.persistence.NewsReactionRepository;
import sn.anaqsup.portail.content.persistence.NewsReactionRepository.Reaction;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Mentions « J'aime » / « Je n'aime pas » d'une actualité, appelées par le portail seulement (chemin signé,
 * voir PortalSignature). Réponse : le nombre de « J'aime » (public) et le vote de ce navigateur ; le nombre de
 * « Je n'aime pas » n'en sort jamais (back-office uniquement, décision ANAQ-Sup du 14/10/2026).
 */
@RestController
@RequestMapping("/api/v1/portal/news/{slug}/reactions")
class NewsReactionsController {

    private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";
    private static final String VISITOR = "X-Anaqsup-Visitor";
    /** Identifiant anonyme tiré au hasard par le portail (UUID). */
    private static final String UUID_PATTERN = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

    /** {@code mine} : LIKE, DISLIKE, ou absent si ce navigateur n'a pas voté. */
    record Reactions(long likes, @Nullable Reaction mine) {
    }

    private final NewsReactionRepository reactions;

    NewsReactionsController(NewsReactionRepository reactions) {
        this.reactions = reactions;
    }

    @GetMapping
    ResponseEntity<Reactions> get(@PathVariable @Pattern(regexp = SLUG) String slug,
                                  @RequestHeader(name = VISITOR, required = false) @Pattern(regexp = UUID_PATTERN) @Nullable String visitor) {
        UUID newsId = newsId(slug);
        Reaction mine = visitor == null ? null : reactions.mine(newsId, fingerprint(visitor)).orElse(null);
        return noStore(new Reactions(reactions.likes(newsId), mine));
    }

    /** Vote (like, dislike), changement d'avis ou annulation (none). */
    @PutMapping("/{reaction}")
    ResponseEntity<Reactions> vote(@PathVariable @Pattern(regexp = SLUG) String slug,
                                   @PathVariable @Pattern(regexp = "like|dislike|none") String reaction,
                                   @RequestHeader(VISITOR) @Pattern(regexp = UUID_PATTERN) String visitor) {
        UUID newsId = newsId(slug);
        byte[] fingerprint = fingerprint(visitor);
        Reaction mine = reaction.equals("none") ? null : Reaction.valueOf(reaction.toUpperCase(Locale.ROOT));
        if (mine == null) {
            reactions.clear(newsId, fingerprint);
        } else {
            reactions.set(newsId, fingerprint, mine);
        }
        return noStore(new Reactions(reactions.likes(newsId), mine));
    }

    private UUID newsId(String slug) {
        return reactions.publishedNewsId(slug).orElseThrow(() -> new ResourceNotFoundException("news", slug));
    }

    /** Seule l'empreinte de l'identifiant est enregistrée : la base ne permet pas de rejouer un cookie. */
    private static byte[] fingerprint(String visitor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(visitor.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Propre à chaque navigateur : jamais mis en cache. */
    private static ResponseEntity<Reactions> noStore(Reactions body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
