package sn.anaqsup.portail.statistics.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.referentiel.ReferentielQueries;

/** Implémente {@code getStatistics} : chiffres recalculés à chaque appel (cache HTTP 15 min). */
@RestController
class PublicStatisticsController {

    private final ReferentielQueries referentiel;
    private final Clock clock;

    PublicStatisticsController(ReferentielQueries referentiel, Clock clock) {
        this.referentiel = referentiel;
        this.clock = clock;
    }

    record StatisticsResponse(Instant computedAt, long habilitatedEstablishments, long accreditedPrograms) {
    }

    @GetMapping("/api/v1/public/statistics")
    ResponseEntity<StatisticsResponse> statistics() {
        var body = new StatisticsResponse(Instant.now(clock), referentiel.countFavorableHabilitations(),
                referentiel.countFavorableAccreditations());
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic()).body(body);
    }
}
