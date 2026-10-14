package sn.anaqsup.portail.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.function.Supplier;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Appels du portail vers l'API ({@code /api/v1/portal/**}, votes des lecteurs) : chemin interne, jamais exposé
 * par Nginx, et signé avec le secret partagé portail ↔ API (REVALIDATE_SECRET), comme le webhook de revalidation.
 * <p>
 * {@code X-Anaqsup-Timestamp} (secondes epoch, 5 minutes d'écart au plus, contre le rejeu) et
 * {@code X-Anaqsup-Signature: sha256=<hex>} = HMAC-SHA256(secret, "timestamp.MÉTHODE.chemin.visiteur"), où
 * « visiteur » est l'en-tête {@code X-Anaqsup-Visitor} (vide s'il est absent).
 */
final class PortalSignature implements AuthorizationManager<RequestAuthorizationContext> {

    static final String TIMESTAMP = "X-Anaqsup-Timestamp";
    static final String SIGNATURE = "X-Anaqsup-Signature";
    static final String VISITOR = "X-Anaqsup-Visitor";
    private static final long MAX_SKEW_SECONDS = 300;

    private final byte[] secret;
    private final Clock clock;

    PortalSignature(String secret, Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        return new AuthorizationDecision(isAuthentic(context.getRequest()));
    }

    private boolean isAuthentic(HttpServletRequest request) {
        String timestamp = request.getHeader(TIMESTAMP);
        String signature = request.getHeader(SIGNATURE);
        // Secret absent ou trop court (configuration incomplète) : tout est refusé.
        if (secret.length < 32 || timestamp == null || signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        long sentAt;
        try {
            sentAt = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (Math.abs(clock.instant().getEpochSecond() - sentAt) > MAX_SKEW_SECONDS) {
            return false;
        }
        String visitor = request.getHeader(VISITOR);
        String payload = timestamp + "." + request.getMethod() + "." + request.getRequestURI() + "." + (visitor == null ? "" : visitor);
        byte[] received;
        try {
            received = HexFormat.of().parseHex(signature.substring("sha256=".length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(hmac(payload), received);
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }
}
