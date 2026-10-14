/**
 * <b>Médias</b> : images téléversées depuis le back-office (photo du Secrétaire exécutif, logos des partenaires,
 * images d'actualités) et leur diffusion publique.
 * <p>
 * Une image est identifiée par une clé (UUID) ; les autres modules ne stockent que cette clé.
 * API publique du module : {@link sn.anaqsup.portail.medias.MediaKeys}.
 */
@ApplicationModule(displayName = "Médias", allowedDependencies = "shared")
package sn.anaqsup.portail.medias;

import org.springframework.modulith.ApplicationModule;
