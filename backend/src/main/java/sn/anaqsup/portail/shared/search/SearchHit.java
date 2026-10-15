package sn.anaqsup.portail.shared.search;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Résultat de la recherche sur tout le site, quel que soit son type (actualité, document, établissement…).
 *
 * @param type    catégorie du contrat ({@code news}, {@code events}, {@code publications}…)
 * @param label   précision lisible (rubrique, lieu, ville, établissement)
 * @param code    valeur codée que le portail traduit (rubrique d'une actualité, type de document, avis)
 * @param fileUrl document : PDF à consulter
 * @param lang    langue servie (repli sur le français si la version anglaise manque)
 */
public record SearchHit(String type, String slug, String title, @Nullable String excerpt, @Nullable LocalDate date,
                        @Nullable String label, @Nullable String code, @Nullable String fileUrl, @Nullable String imageUrl,
                        String lang) {

    /**
     * Résultat avec ce qui le classe parmi ceux des autres types : titre correspondant d'abord, puis pertinence
     * (rapportée à la longueur du texte, donc comparable d'une table à l'autre), puis le plus récent.
     */
    public record Scored(SearchHit hit, boolean titleMatch, double rank, @Nullable Instant sortDate) {

        public static final Comparator<Scored> RELEVANCE = Comparator.comparing(Scored::titleMatch).reversed()
                .thenComparing(Comparator.comparingDouble(Scored::rank).reversed())
                .thenComparing(Scored::sortDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(scored -> scored.hit().title());
    }

    /** Les premiers résultats d'un type, dans l'ordre {@link Scored#RELEVANCE}, avec leur nombre total. */
    public record Slice(List<Scored> items, long total) {
    }
}
