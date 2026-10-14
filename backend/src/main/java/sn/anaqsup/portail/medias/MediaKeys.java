package sn.anaqsup.portail.medias;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/** Conversion d'une clé de média en adresse publique, utilisable par tous les modules. */
public final class MediaKeys {

    private MediaKeys() {
    }

    /**
     * Adresse publique d'une image :
     * <ul>
     *   <li>clé téléversée (UUID) → {@code /media/<uuid>}, servie par le portail (même origine que le site) ;</li>
     *   <li>adresse déjà complète (illustrations de démonstration {@code /demo/…}, ou {@code https://…}) → telle quelle.</li>
     * </ul>
     */
    public static @Nullable String publicUrl(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("/") || key.startsWith("https://")) {
            return key;
        }
        return isUuid(key) ? "/media/" + key : null;
    }

    public static boolean isUuid(@Nullable String key) {
        if (key == null) {
            return false;
        }
        try {
            UUID.fromString(key);
            return key.length() == 36;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
