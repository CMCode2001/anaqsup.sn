/**
 * Contenu du <b>site institutionnel</b> : actualités et catégories, événements, ressources documentaires,
 * pages institutionnelles (présentation, missions, gouvernance, organigramme, mot du Secrétaire exécutif)
 * et partenaires.
 * <p>
 * Publication directe (décision ANAQ-Sup) ; le HTML saisi dans le back-office est assaini à l'écriture.
 * L'API publique ne renvoie que les contenus publiés. Chaque modification publie
 * {@link sn.anaqsup.portail.content.ContentChanged} pour que le portail se mette à jour.
 */
@ApplicationModule(displayName = "Contenu institutionnel", allowedDependencies = {"shared", "medias"})
package sn.anaqsup.portail.content;

import org.springframework.modulith.ApplicationModule;
