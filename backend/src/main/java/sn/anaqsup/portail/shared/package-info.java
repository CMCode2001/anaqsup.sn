/**
 * Noyau technique partagé : format d'erreur RFC 9457, sécurité, configuration web, horloge.
 * <p>
 * Module <b>ouvert</b> : tous les modules peuvent en dépendre. Il ne doit contenir
 * <b>aucune règle métier</b>.
 */
@ApplicationModule(displayName = "Noyau partagé", type = ApplicationModule.Type.OPEN)
package sn.anaqsup.portail.shared;

import org.springframework.modulith.ApplicationModule;
