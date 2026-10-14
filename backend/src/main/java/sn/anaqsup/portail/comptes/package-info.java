/**
 * <b>Comptes du back-office</b> : comptes simples gérés par le backend (ADR-0006), rôles et permissions,
 * connexion par session.
 * <p>
 * Rôles : « Administrateur » (tous les droits, non modifiable) et « Chargé de communication » ;
 * l'administrateur peut créer d'autres rôles. API publique du module : {@link sn.anaqsup.portail.comptes.Permission}
 * (autorités {@code PERM_*} utilisables par les autres modules pour protéger leurs écrans d'administration).
 */
@ApplicationModule(displayName = "Comptes du back-office", allowedDependencies = "shared")
package sn.anaqsup.portail.comptes;

import org.springframework.modulith.ApplicationModule;
