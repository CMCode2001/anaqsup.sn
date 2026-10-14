package sn.anaqsup.portail.shared.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** Adresses (slugs) des contenus : minuscules sans accents, mots séparés par des tirets. */
public final class Slugs {

    private static final int MAX_LENGTH = 80;

    private Slugs() {
    }

    /** « Référentiel d'évaluation (2026) » → referentiel-d-evaluation-2026 ; vide → « document ». */
    public static String from(String text) {
        String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "document" : slug;
    }

    /** Premier slug libre parmi base, base-2, base-3… */
    public static String unique(String base, Predicate<String> taken) {
        String slug = base;
        for (int n = 2; taken.test(slug); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }
}
