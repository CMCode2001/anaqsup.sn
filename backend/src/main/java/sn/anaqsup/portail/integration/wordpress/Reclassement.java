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
    private static final VersDocuments DISCOURS = new VersDocuments("DISCOURS", null);

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
            // Autre rapport, même catégorie : « L'Enseignement de la gestion au Sénégal : un état des lieux ».
            Map.entry(5697L, RAPPORT),
            // Discours et allocutions : remise des rapports au Ministre (SE), CIDAQ 2023 (Ministre), lancement de
            // QADoc (SE), ouverture de l'atelier CNC-Q.
            Map.entry(1599L, DISCOURS),
            Map.entry(5150L, DISCOURS),
            Map.entry(5752L, DISCOURS),
            Map.entry(5783L, DISCOURS),
            // Courrier du Ministre aux établissements privés d'enseignement supérieur.
            Map.entry(2867L, new VersDocuments("AUTRE", null)),
            // Déjà dans les ressources (reprises depuis les pages de documents) : non recréés, actualités retirées.
            Map.entry(2856L, new VersDocuments("GUIDE", null)), // Guide d'auto-évaluation école doctorale
            Map.entry(5687L, new VersDocuments("ETUDE", "Analyse situationnelle de la recherche au Sénégal")),
            Map.entry(5692L, new VersDocuments("ETUDE", "Analyse situationnelle de la recherche au Sénégal")),
            // Non repris.
            Map.entry(1597L, new Retire("rapport d'activités 2017 sans PDF, même sur l'ancien site")),
            Map.entry(2847L, new Retire("titre de discours, mais lien vers le rapport 2014 : doublon")));

    private Reclassement() {
    }
}
