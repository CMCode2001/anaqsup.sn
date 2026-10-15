/**
 * Recherche sur tout le site (décision ANAQ-Sup du 14/10/2026) : actualités, événements, documents, pages,
 * établissements et formations, en une seule requête. Lit les autres modules par leurs services publics.
 */
@ApplicationModule(displayName = "Recherche", allowedDependencies = {"shared", "content", "referentiel"})
package sn.anaqsup.portail.search;

import org.springframework.modulith.ApplicationModule;
