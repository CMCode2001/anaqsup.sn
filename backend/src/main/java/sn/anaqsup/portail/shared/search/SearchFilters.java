package sn.anaqsup.portail.shared.search;

import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

/**
 * Filtres de la page de résultats. Un type qui ne connaît pas un filtre demandé est écarté des résultats
 * (une période écarte les pages et le registre, qui n'ont pas de date).
 *
 * @param from     première date incluse (publication, début de l'événement, date du document)
 * @param until    première date exclue
 * @param category rubrique d'actualités (slug)
 * @param kind     type de document ({@code PublicationKind})
 */
public record SearchFilters(@Nullable LocalDate from, @Nullable LocalDate until, @Nullable String category,
                            @Nullable String kind) {

    public static final SearchFilters NONE = new SearchFilters(null, null, null, null);

    public boolean dated() {
        return from != null || until != null;
    }
}
