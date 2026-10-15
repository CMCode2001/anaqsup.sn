package sn.anaqsup.portail.shared.search;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Recherche « au fil de la frappe » sur tout le site : chaque mot devient un préfixe (« rapp » trouve « rapport »,
 * « rapports »…) et tous les mots doivent être présents. Les accents sont ignorés côté SQL
 * ({@code to_tsquery('french', immutable_unaccent(:tsquery))}), les mots d'une lettre (« l' », « d' ») aussi.
 *
 * @param tsquery requête PostgreSQL, sans caractère spécial autre que les opérateurs ajoutés ici
 */
public record SearchTerms(String tsquery) {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]{2,}");
    private static final int MAX_WORDS = 8;

    public static Optional<SearchTerms> of(String text) {
        List<String> words = WORD.matcher(text.toLowerCase(Locale.ROOT)).results().map(MatchResult::group)
                .distinct().limit(MAX_WORDS).toList();
        return words.isEmpty() ? Optional.empty()
                : Optional.of(new SearchTerms(words.stream().map(word -> word + ":*").collect(Collectors.joining(" & "))));
    }
}
