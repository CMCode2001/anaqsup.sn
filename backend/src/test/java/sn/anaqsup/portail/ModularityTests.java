package sn.anaqsup.portail;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/** Garde-fou d'architecture : une dépendance non déclarée entre modules fait échouer la CI. */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(PortailApplication.class);

    @Test
    void respecteLesFrontieresDesModules() {
        modules.verify();
    }

    @Test
    void genereLaDocumentationDesModules() {
        // Diagrammes C4/PlantUML + canevas des modules dans target/spring-modulith-docs
        new Documenter(modules).writeDocumentation();
    }
}
