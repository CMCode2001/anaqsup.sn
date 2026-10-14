package sn.anaqsup.portail.referentiel.web;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.referentiel.persistence.RegistryRepository;
import sn.anaqsup.portail.referentiel.web.RegistryResponses.Establishment;
import sn.anaqsup.portail.referentiel.web.RegistryResponses.EstablishmentSummary;
import sn.anaqsup.portail.referentiel.web.RegistryResponses.ProgramSummary;
import sn.anaqsup.portail.shared.web.PageResponse;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/** Listes des habilitations (établissements) et accréditations (formations) du contrat v1. */
@RestController
@RequestMapping("/api/v1/public")
class PublicRegistryController {

    private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";
    private static final String AVIS = "FAVORABLE|DEFAVORABLE";
    private static final CacheControl PUBLIC_5_MIN = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();

    private final RegistryRepository registry;

    PublicRegistryController(RegistryRepository registry) {
        this.registry = registry;
    }

    @GetMapping("/establishments")
    ResponseEntity<PageResponse<EstablishmentSummary>> establishments(
            @RequestParam(required = false) @Size(max = 100) @Nullable String q,
            @RequestParam(required = false) @Size(max = 80) @Nullable String city,
            @RequestParam(required = false) @Pattern(regexp = "PUBLIC|PRIVE") @Nullable String kind,
            @RequestParam(required = false) @Pattern(regexp = AVIS) @Nullable String avis,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var slice = registry.searchEstablishments(blankToNull(q), blankToNull(city), kind, avis, page, size);
        var content = slice.rows().stream().map(EstablishmentSummary::from).toList();
        return ok(PageResponse.of(content, page, size, slice.total()));
    }

    @GetMapping("/establishments/{slug}")
    ResponseEntity<Establishment> establishment(@PathVariable @Pattern(regexp = SLUG) String slug) {
        var row = registry.findEstablishment(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Établissement", slug));
        return ok(Establishment.from(row, registry.countPrograms(row.id())));
    }

    @GetMapping("/programs")
    ResponseEntity<PageResponse<ProgramSummary>> programs(
            @RequestParam(required = false) @Size(max = 100) @Nullable String q,
            @RequestParam(required = false) @Pattern(regexp = SLUG) @Nullable String establishment,
            @RequestParam(required = false) @Pattern(regexp = "LICENCE|MASTER|DOCTORAT|DUT|BTS|AUTRE") @Nullable String level,
            @RequestParam(required = false) @Pattern(regexp = AVIS) @Nullable String avis,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var slice = registry.searchPrograms(blankToNull(q), establishment, level, avis, page, size);
        var content = slice.rows().stream().map(ProgramSummary::from).toList();
        return ok(PageResponse.of(content, page, size, slice.total()));
    }

    @GetMapping("/programs/{slug}")
    ResponseEntity<ProgramSummary> program(@PathVariable @Pattern(regexp = SLUG) String slug) {
        return ok(registry.findProgram(slug).map(ProgramSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Formation", slug)));
    }

    private static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok().cacheControl(PUBLIC_5_MIN).body(body);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
