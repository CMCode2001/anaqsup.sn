/**
 * <b>Habilitations et accréditations</b> : listes structurées des établissements et des formations,
 * avec l'avis rendu par l'ANAQ-Sup (favorable / défavorable) et son année, saisis tels quels.
 * <p>
 * API publique du module : {@link sn.anaqsup.portail.referentiel.ReferentielQueries}.
 * Les sous-packages sont internes.
 */
@ApplicationModule(displayName = "Habilitations et accréditations", allowedDependencies = "shared")
package sn.anaqsup.portail.referentiel;

import org.springframework.modulith.ApplicationModule;
