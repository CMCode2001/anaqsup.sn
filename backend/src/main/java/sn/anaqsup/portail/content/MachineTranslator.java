package sn.anaqsup.portail.content;

import java.util.List;

/**
 * Traduction automatique du français vers l'anglais (décision ANAQ-Sup du 14/10/2026 : DeepL).
 * Implémentée par le module Intégrations ; le module Contenu ne connaît que cette interface.
 */
public interface MachineTranslator {

    /** Faux tant qu'aucune clé n'est configurée : les contenus restent alors en français sur le site anglais. */
    boolean available();

    /**
     * Traduit les textes, dans le même ordre. {@code html} : textes HTML dont les balises doivent être conservées.
     *
     * @throws UnavailableException service injoignable, clé refusée ou quota mensuel atteint
     */
    List<String> toEnglish(List<String> texts, boolean html);

    final class UnavailableException extends RuntimeException {

        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnavailableException(String message) {
            super(message);
        }
    }
}
