package sn.anaqsup.portail.comptes;

/** Droits attribuables à un rôle. Un compte a les droits de son rôle, et seulement ceux-là. */
public enum Permission {
    /** Gérer les comptes et les rôles. */
    COMPTES,
    ACTUALITES,
    EVENEMENTS,
    /** Ressources documentaires (publications, textes réglementaires, référentiels, guides). */
    DOCUMENTS,
    /** Pages institutionnelles (présentation, missions, gouvernance, organigramme, mot du SE, fiches procédures). */
    PAGES,
    /** Avis d'habilitation et d'accréditation. */
    AVIS,
    /** Partenaires affichés sur l'accueil. */
    PARTENAIRES,
    /** Paramètres du site : coordonnées, réseaux sociaux, liens utiles, textes d'introduction des rubriques. */
    PARAMETRES;

    /** Autorité Spring Security correspondante, ex. {@code PERM_ACTUALITES}. */
    public String authority() {
        return "PERM_" + name();
    }
}
