package sn.anaqsup.portail.shared.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Horloge unique de l'application : tout calcul « à date » (statut en vigueur, expiration)
 * passe par ce bean, ce qui rend les règles testables à une date fixe.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

    static final ZoneId DAKAR = ZoneId.of("Africa/Dakar");

    @Bean
    Clock clock() {
        return Clock.system(DAKAR);
    }
}
