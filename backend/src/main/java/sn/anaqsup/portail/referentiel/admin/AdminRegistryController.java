package sn.anaqsup.portail.referentiel.admin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.referentiel.RegistryChanged;
import sn.anaqsup.portail.referentiel.persistence.AdminRegistryRepository;
import sn.anaqsup.portail.referentiel.persistence.AdminRegistryRepository.EstablishmentChanges;
import sn.anaqsup.portail.referentiel.persistence.AdminRegistryRepository.EstablishmentRow;
import sn.anaqsup.portail.referentiel.persistence.AdminRegistryRepository.ProgramChanges;
import sn.anaqsup.portail.referentiel.persistence.AdminRegistryRepository.ProgramRow;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository;
import sn.anaqsup.portail.shared.text.Slugs;
import sn.anaqsup.portail.shared.web.PageResponse;
import sn.anaqsup.portail.shared.web.Problems;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Back-office : établissements, composantes et formations (permission AVIS).
 * <p>
 * L'avis d'un établissement ou d'une formation reflète sa décision validée la plus récente (rubrique
 * {@code /decisions}). Pour rester compatible avec la saisie rapide de la fiche, un avis et une année envoyés ici
 * créent une décision validée « saisie rapide » ; retirer un avis, ou le dater avant la dernière décision, est refusé.
 * Publication directe : le portail est prévenu à chaque modification.
 */
@RestController
@RequestMapping("/api/v1/admin/registry")
class AdminRegistryController {

    private static final String AVIS = "FAVORABLE|DEFAVORABLE";
    private static final String NATURE = "UNIVERSITE|INSTITUT|ECOLE|CENTRE|UFR_FACULTE|ECOLE_DOCTORALE|AUTRE";
    static final String LEVEL = "LICENCE|MASTER|DOCTORAT|DUT|BTS|AUTRE";
    private static final int HABILITATION_YEARS = 10;
    private static final int ACCREDITATION_YEARS = 5;

    record Establishment(UUID id, String slug, String name, @Nullable String acronym, String kind, @Nullable String city,
                         @Nullable String region, @Nullable String website, @Nullable String avis, @Nullable Integer avisYear,
                         long programs, String nature, @Nullable UUID parentId, boolean provisional) {
        static Establishment from(EstablishmentRow r) {
            return new Establishment(r.id(), r.slug(), r.name(), r.acronym(), r.kind(), r.city(), r.region(), r.website(),
                    r.avis(), r.avisYear(), r.programs(), r.nature(), r.parentId(), r.provisional());
        }
    }

    record EstablishmentRequest(@NotBlank @Size(max = 300) String name,
                                @Nullable @Size(max = 40) String acronym,
                                @NotBlank @Pattern(regexp = "PUBLIC|PRIVE") String kind,
                                @Nullable @Size(max = 120) String city,
                                @Nullable @Size(max = 120) String region,
                                @Nullable @Size(max = 300) String website,
                                @Nullable @Pattern(regexp = AVIS) String avis,
                                @Nullable @Min(2000) @Max(2100) Integer avisYear,
                                @Nullable @Pattern(regexp = NATURE) String nature,
                                @Nullable UUID parentId) {
    }

    record Program(UUID id, String slug, String title, String level, @Nullable String domain, UUID establishmentId,
                   String establishmentName, @Nullable String establishmentAcronym, @Nullable String avis,
                   @Nullable Integer avisYear, @Nullable String optionParcours, boolean provisional) {
        static Program from(ProgramRow r) {
            return new Program(r.id(), r.slug(), r.title(), r.level(), r.domain(), r.establishmentId(), r.establishmentName(),
                    r.establishmentAcronym(), r.avis(), r.avisYear(), r.optionParcours(), r.provisional());
        }
    }

    record ProgramRequest(@NotBlank @Size(max = 300) String title,
                          @NotBlank @Pattern(regexp = LEVEL) String level,
                          @Nullable @Size(max = 200) String domain,
                          @NotNull UUID establishmentId,
                          @Nullable @Pattern(regexp = AVIS) String avis,
                          @Nullable @Min(2000) @Max(2100) Integer avisYear,
                          @Nullable @Size(max = 200) String optionParcours) {
    }

    private final AdminRegistryRepository repository;
    private final DecisionRepository decisions;
    private final ApplicationEventPublisher events;

    AdminRegistryController(AdminRegistryRepository repository, DecisionRepository decisions, ApplicationEventPublisher events) {
        this.repository = repository;
        this.decisions = decisions;
        this.events = events;
    }

    // --------------------------------------------------------------------------------- établissements

    @GetMapping("/establishments")
    PageResponse<Establishment> establishments(@RequestParam(required = false) @Size(max = 100) @Nullable String q,
                                               @RequestParam(required = false) @Pattern(regexp = AVIS + "|AUCUN") @Nullable String avis,
                                               @RequestParam(defaultValue = "0") @Min(0) int page,
                                               @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        var slice = repository.establishments(q, avis, page, size);
        return PageResponse.of(slice.rows().stream().map(Establishment::from).toList(), page, size, slice.total());
    }

    @GetMapping("/establishments/{id}")
    Establishment establishment(@PathVariable UUID id) {
        return Establishment.from(findEstablishment(id));
    }

    @PostMapping("/establishments")
    @Transactional
    ResponseEntity<Establishment> createEstablishment(@Valid @RequestBody EstablishmentRequest body, Authentication authentication) {
        var changes = changes(body, null);
        String slug = Slugs.unique(Slugs.from(changes.acronym() != null ? changes.acronym() + " " + changes.name() : changes.name()),
                repository::establishmentSlugTaken);
        UUID id = repository.insertEstablishment(slug, changes);
        quickAvis(id, null, null, null, body.avis(), body.avisYear(), HABILITATION_YEARS, authentication.getName());
        announce(List.of("establishments", "establishment:" + slug));
        return ResponseEntity.created(URI.create("/api/v1/admin/registry/establishments/" + id))
                .body(Establishment.from(repository.establishment(id).orElseThrow()));
    }

    @PutMapping("/establishments/{id}")
    @Transactional
    Establishment updateEstablishment(@PathVariable UUID id, @Valid @RequestBody EstablishmentRequest body,
                                      Authentication authentication) {
        EstablishmentRow current = findEstablishment(id);
        repository.updateEstablishment(id, changes(body, id));
        quickAvis(id, null, current.avis(), current.avisYear(), body.avis(), body.avisYear(), HABILITATION_YEARS,
                authentication.getName());
        // Les fiches des formations affichent le nom de l'établissement.
        announce(List.of("establishments", "establishment:" + current.slug(), "programs"));
        return Establishment.from(repository.establishment(id).orElseThrow());
    }

    /** Retire le rattachement d'une composante à son établissement. */
    @DeleteMapping("/establishments/{id}/parent")
    @Transactional
    Establishment detachEstablishment(@PathVariable UUID id) {
        EstablishmentRow current = findEstablishment(id);
        repository.clearParent(id);
        announce(List.of("establishments", "establishment:" + current.slug(), "programs"));
        return Establishment.from(repository.establishment(id).orElseThrow());
    }

    @DeleteMapping("/establishments/{id}")
    @Transactional
    ResponseEntity<Void> deleteEstablishment(@PathVariable UUID id) {
        EstablishmentRow current = findEstablishment(id);
        if (current.programs() > 0) {
            throw invalid("ESTABLISHMENT_HAS_PROGRAMS", "Supprimez ou rattachez ailleurs ses formations d'abord.");
        }
        if (repository.componentCount(id) > 0) {
            throw invalid("ESTABLISHMENT_HAS_COMPONENTS", "Détachez ou supprimez ses composantes d'abord.");
        }
        if (decisions.establishmentHasKeptDecisions(id)) {
            throw invalid("ESTABLISHMENT_HAS_DECISIONS", "Il a des décisions : archivez-les dans la rubrique Décisions plutôt que de le supprimer.");
        }
        decisions.deleteQuickDecisionsOfEstablishment(id);   // un avis saisi depuis la fiche part avec elle
        repository.deleteEstablishment(id);
        announce(List.of("establishments", "establishment:" + current.slug()));
        return ResponseEntity.noContent().build();
    }

    private EstablishmentRow findEstablishment(UUID id) {
        return repository.establishment(id).orElseThrow(() -> new ResourceNotFoundException("Établissement", id.toString()));
    }

    private EstablishmentChanges changes(EstablishmentRequest b, @Nullable UUID selfId) {
        String website = blankToNull(b.website());
        if (website != null && !website.matches("^https?://\\S+$")) {
            throw invalid("INVALID_URL", "L'adresse du site doit commencer par https://");
        }
        if (b.parentId() != null) {
            String parentKind = repository.kindOf(b.parentId())
                    .orElseThrow(() -> invalid("UNKNOWN_PARENT", "Établissement parent inconnu."));
            if (selfId != null && repository.isSelfOrDescendant(selfId, b.parentId())) {
                throw invalid("PARENT_CYCLE", "Un établissement ne peut pas être sa propre composante.");
            }
            if (!parentKind.equals(b.kind())) {
                throw invalid("PARENT_KIND_MISMATCH", "Une composante a le même type (public ou privé) que son établissement.");
            }
        }
        return new EstablishmentChanges(b.name().strip(), blankToNull(b.acronym()), b.kind(), blankToNull(b.city()),
                blankToNull(b.region()), website, b.nature(), b.parentId());
    }

    // ------------------------------------------------------------------------------------- formations

    @GetMapping("/programs")
    PageResponse<Program> programs(@RequestParam(required = false) @Size(max = 100) @Nullable String q,
                                   @RequestParam(required = false) @Nullable UUID establishment,
                                   @RequestParam(required = false) @Pattern(regexp = AVIS + "|AUCUN") @Nullable String avis,
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        var slice = repository.programs(q, establishment, avis, page, size);
        return PageResponse.of(slice.rows().stream().map(Program::from).toList(), page, size, slice.total());
    }

    @GetMapping("/programs/{id}")
    Program program(@PathVariable UUID id) {
        return Program.from(findProgram(id));
    }

    @PostMapping("/programs")
    @Transactional
    ResponseEntity<Program> createProgram(@Valid @RequestBody ProgramRequest body, Authentication authentication) {
        EstablishmentRow establishment = findEstablishmentFor(body.establishmentId());
        var changes = changes(body);
        String base = (establishment.acronym() != null ? establishment.acronym() : establishment.name()) + " " + changes.title()
                + (changes.optionParcours() != null ? " " + changes.optionParcours() : "");
        String slug = Slugs.unique(Slugs.from(base), repository::programSlugTaken);
        UUID id = repository.insertProgram(slug, changes);
        quickAvis(establishment.id(), id, null, null, body.avis(), body.avisYear(), ACCREDITATION_YEARS, authentication.getName());
        announce(List.of("programs", "program:" + slug, "establishment:" + establishment.slug()));
        return ResponseEntity.created(URI.create("/api/v1/admin/registry/programs/" + id))
                .body(Program.from(repository.program(id).orElseThrow()));
    }

    @PutMapping("/programs/{id}")
    @Transactional
    Program updateProgram(@PathVariable UUID id, @Valid @RequestBody ProgramRequest body, Authentication authentication) {
        ProgramRow current = findProgram(id);
        EstablishmentRow establishment = findEstablishmentFor(body.establishmentId());
        repository.updateProgram(id, changes(body));
        quickAvis(establishment.id(), id, current.avis(), current.avisYear(), body.avis(), body.avisYear(), ACCREDITATION_YEARS,
                authentication.getName());
        List<String> tags = new ArrayList<>(List.of("programs", "program:" + current.slug(), "establishment:" + establishment.slug()));
        if (!establishment.slug().equals(current.establishmentSlug())) {
            tags.add("establishment:" + current.establishmentSlug()); // formation rattachée à un autre établissement
        }
        announce(tags);
        return Program.from(repository.program(id).orElseThrow());
    }

    @DeleteMapping("/programs/{id}")
    @Transactional
    ResponseEntity<Void> deleteProgram(@PathVariable UUID id) {
        ProgramRow current = findProgram(id);
        if (decisions.programHasKeptDecisions(id)) {
            throw invalid("PROGRAM_HAS_DECISIONS", "Elle apparaît dans des décisions : archivez-les plutôt que de la supprimer.");
        }
        decisions.deleteQuickDecisionsOfProgram(id);   // un avis saisi depuis la fiche part avec elle
        repository.deleteProgram(id);
        announce(List.of("programs", "program:" + current.slug(), "establishment:" + current.establishmentSlug()));
        return ResponseEntity.noContent().build();
    }

    private ProgramRow findProgram(UUID id) {
        return repository.program(id).orElseThrow(() -> new ResourceNotFoundException("Formation", id.toString()));
    }

    private EstablishmentRow findEstablishmentFor(UUID id) {
        return repository.establishment(id).orElseThrow(() -> invalid("UNKNOWN_ESTABLISHMENT", "Établissement inconnu."));
    }

    private static ProgramChanges changes(ProgramRequest b) {
        return new ProgramChanges(b.title().strip(), b.level(), blankToNull(b.domain()), b.establishmentId(),
                blankToNull(b.optionParcours()));
    }

    // ------------------------------------------------------------------------------------------ règles

    /**
     * Saisie rapide d'un avis depuis la fiche : crée une décision validée (avis + année, sans date de Conseil scientifique
     * ni rapport). Sans changement, rien n'est fait. Pour retirer un avis ou corriger l'historique, utiliser les décisions.
     */
    private void quickAvis(UUID establishmentId, @Nullable UUID programId, @Nullable String currentAvis,
                           @Nullable Integer currentYear, @Nullable String avis, @Nullable Integer year, int validityYears,
                           String actor) {
        if ((avis == null) != (year == null)) {
            throw invalid("AVIS_INCOMPLETE", "Indiquez l'avis ET son année (ou ni l'un ni l'autre).");
        }
        if (Objects.equals(avis, currentAvis) && Objects.equals(year, currentYear)) {
            return;
        }
        if (avis == null) {
            throw invalid("AVIS_REMOVAL_VIA_DECISIONS",
                    "Pour retirer un avis, archivez sa décision dans la rubrique Décisions.");
        }
        decisions.latestValidYear(programId == null ? establishmentId : null, programId).ifPresent(latest -> {
            if (year < latest) {
                throw invalid("NEWER_DECISION_EXISTS", "Une décision plus récente (" + latest
                        + ") existe : saisissez une nouvelle décision plutôt que d'antidater l'avis.");
            }
        });
        decisions.insertValidatedQuick(programId == null ? "HABILITATION" : "ACCREDITATION", establishmentId, programId, avis,
                year, validityYears, actor);
    }

    private void announce(List<String> tags) {
        events.publishEvent(new RegistryChanged(tags));
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static ErrorResponseException invalid(String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(Problems.type("admin/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle("Requête refusée");
        problem.setProperty("code", code);
        return new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
    }
}
