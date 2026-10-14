/**
 * Adaptateurs techniques du portail anaqsup.sn :
 * <ul>
 *   <li>webhook signé de revalidation du portail ({@link sn.anaqsup.portail.integration.PortalRevalidation}),
 *       appelé après chaque modification faite dans le back-office ;</li>
 *   <li>traduction automatique vers l'anglais ({@link sn.anaqsup.portail.integration.DeeplTranslator}) ;</li>
 *   <li>reprise des actualités et des documents de l'ancien site WordPress (paquet {@code wordpress}),
 *       lancée ponctuellement ; les listes d'habilitations et d'accréditations restent à reprendre après curation.</li>
 * </ul>
 * Le portail ne dépend d'aucun système externe pour ses données.
 */
@ApplicationModule(displayName = "Intégrations", allowedDependencies = {"shared", "content", "medias", "referentiel"})
package sn.anaqsup.portail.integration;

import org.springframework.modulith.ApplicationModule;
