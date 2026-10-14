package sn.anaqsup.portail.integration.wordpress;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Articles de l'ancien site publiés par erreur en « Communiqués » (décision ANAQ-Sup du 14/10/2026). Ce sont des
 * documents, repris dans Ressources documentaires → Publications avec leur PDF, ou des articles à ne pas reprendre.
 * Un article déjà repris en actualité par un import précédent est déplacé ou retiré de la même façon.
 */
final class Reclassement {

    sealed interface Action permits VersDocuments, Retire {
    }

    /** @param title titre corrigé, ou null pour garder celui de l'ancien site */
    record VersDocuments(String kind, @Nullable String title) implements Action {
    }

    record Retire(String motif) implements Action {
    }

    private static final VersDocuments RAPPORT = new VersDocuments("RAPPORT_ANNUEL", null);

    /** Par identifiant WordPress de l'article. */
    static final Map<Long, Action> ACTIONS = Map.ofEntries(
            // Rapports d'activités annuels (catégorie « Rapport »).
            Map.entry(2795L, RAPPORT), // 2014
            Map.entry(2797L, RAPPORT), // 2015
            Map.entry(2799L, RAPPORT), // 2016
            Map.entry(1593L, RAPPORT), // 2018
            Map.entry(1591L, RAPPORT), // 2019
            Map.entry(1595L, RAPPORT), // 2020
            Map.entry(2791L, RAPPORT), // 2021
            Map.entry(2793L, RAPPORT), // 2022
            // 2023 : titre d'origine fautif (« DEL’ANNÉE ») ; non recréé s'il existe déjà sous ce titre.
            Map.entry(6388L, new VersDocuments("RAPPORT_ANNUEL", "RAPPORT D’ACTIVITÉS ET DE RÉSULTATS DE L’ANNÉE 2023")),
            // Autres rapports, même catégorie : CNAES, Colloque international, évaluation externe de l'ANAQ-Sup.
            Map.entry(2864L, RAPPORT),
            Map.entry(2870L, RAPPORT),
            Map.entry(2873L, RAPPORT),
            // Discours du Secrétaire exécutif (remise des rapports au Ministre).
            Map.entry(1599L, new VersDocuments("DISCOURS", null)),
            // Non repris.
            Map.entry(1597L, new Retire("rapport d'activités 2017 sans PDF, même sur l'ancien site")),
            Map.entry(2847L, new Retire("titre de discours, mais lien vers le rapport 2014 : doublon")));

    private Reclassement() {
    }
}
