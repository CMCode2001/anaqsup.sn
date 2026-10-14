/**
 * Journal <b>append-only</b> des actions sensibles (publication, rectificatif, droits),
 * alimenté par écoute des événements des autres modules. Table protégée par trigger.
 */
@ApplicationModule(displayName = "Journal d'audit")
package sn.anaqsup.portail.audit;

import org.springframework.modulith.ApplicationModule;
