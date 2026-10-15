package sn.anaqsup.portail.referentiel.admin;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.springframework.dao.DataIntegrityViolationException;
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
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.DecisionInput;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.DecisionRow;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.ProgramAvisInput;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.ProgramAvisRow;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.ReportInput;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.ReportRow;
import sn.anaqsup.portail.shared.web.PageResponse;
import sn.anaqsup.portail.shared.web.Problems;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Back-office : décisions du Conseil scientifique (permission AVIS, comme le reste du registre).
 * <p>
 * Une décision naît en <b>brouillon</b> (invisible du public), se corrige tant qu'elle est brouillon, puis se
 * <b>valide</b> (publication directe, sans relecture obligatoire : décision de l'ANAQ-Sup) ou se supprime. Une décision
 * validée ne se modifie plus : on l'<b>archive</b> et on en crée une nouvelle, pour garder l'historique.
 * Tous les contrôles bloquants sont faits avant la moindre écriture et listés ensemble dans la réponse 422.
 */
@RestController
@RequestMapping("/api/v1/admin/registry/decisions")
class AdminDecisionController {

    private static final String AVIS = "FAVORABLE|DEFAVORABLE|CONDITIONNEL";
    private static final String KIND = "HABILITATION|HABILITATION_COMPLEMENTAIRE|ACCREDITATION|RENOUVELLEMENT_ACCREDITATION";
    private static final String STATUS = "BROUILLON|VALIDE|ARCHIVE";
    private static final Set<String> KINDS_WITH_PROGRAMS = Set.of("ACCREDITATION", "RENOUVELLEMENT_ACCREDITATION");
    private static final String OFFICIAL_HOST = "anaqsup.sn";

    record ReportRequest(@NotBlank @Size(max = 500) String title,
                         @NotBlank @Size(max = 2000) String url,
                         @Nullable @Pattern(regexp = "RAPPORT_EVALUATION_EXTERNE|DECISION_CONSEIL_SCIENTIFIQUE|AUTRE") String kind,
                         @Nullable LocalDate signedOn,
                         @Nullable LocalDate visitedOn,
                         @Nullable @Pattern(regexp = "PUBLIC|PRIVE") String visibility) {
    }

    record ProgramAvisRequest(@NotNull UUID programId,
                              @NotBlank @Pattern(regexp = AVIS) String avis,
                              @Nullable UUID reportId,
                              @Nullable @Size(max = 2000) String remark) {
    }

    /** {@code year} est obligatoire sans {@code decidedOn} (l'ancien site ne donne que l'année) et déduit de la date sinon. */
    record DecisionRequest(@NotBlank @Pattern(regexp = KIND) String kind,
                           @NotNull UUID establishmentId,
                           @Nullable LocalDate decidedOn,
                           @Nullable @Min(2014) @Max(2100) Integer year,
                           @NotNull @Min(1) @Max(20) Integer validityYears,
                           @Nullable @Min(1) @Max(99) Integer wave,
                           @Nullable @Pattern(regexp = AVIS) String avis,
                           @Nullable @Valid ReportRequest report,
                           @Nullable UUID previousId,
                           @Nullable @Size(max = 5000) String comment,
                           @Nullable @Valid List<ProgramAvisRequest> programs) {
    }

    record Report(UUID id, String kind, String title, String url, @Nullable LocalDate signedOn,
                  @Nullable LocalDate visitedOn, String visibility, @Nullable Boolean linkOk) {
        static Report from(ReportRow r) {
            return new Report(r.id(), r.kind(), r.title(), r.url(), r.signedOn(), r.visitedOn(), r.visibility(), r.linkOk());
        }
    }

    record ProgramAvis(UUID programId, String programSlug, String title, String avis, @Nullable UUID reportId,
                       @Nullable String remark) {
        static ProgramAvis from(ProgramAvisRow r) {
            return new ProgramAvis(r.programId(), r.programSlug(), r.title(), r.avis(), r.reportId(), r.remark());
        }
    }

    record Decision(UUID id, String kind, UUID establishmentId, String establishmentSlug, String establishmentName,
                    @Nullable LocalDate decidedOn, int year, int validityYears, LocalDate validUntil,
                    boolean validUntilEstimated, @Nullable Integer wave, @Nullable String avis, @Nullable String effectiveAvis,
                    String status, @Nullable String comment, @Nullable UUID previousId, @Nullable Report report,
                    List<ProgramAvis> programs, String enteredBy, @Nullable String validatedBy, OffsetDateTime createdAt,
                    OffsetDateTime updatedAt, List<String> warnings) {
    }

    record DecisionItem(UUID id, String kind, UUID establishmentId, String establishmentSlug, String establishmentName,
                        int year, @Nullable LocalDate decidedOn, LocalDate validUntil, @Nullable String effectiveAvis,
                        String status, String enteredBy, OffsetDateTime updatedAt) {
        static DecisionItem from(DecisionRow d) {
            return new DecisionItem(d.id(), d.kind(), d.establishmentId(), d.establishmentSlug(), d.establishmentName(),
                    d.displayYear(), d.decidedOn(), d.validUntil(), d.effectiveAvis(), d.status(), d.enteredBy(), d.updatedAt());
        }
    }

    private final DecisionRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    AdminDecisionController(DecisionRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @GetMapping
    PageResponse<DecisionItem> list(@RequestParam(required = false) @Nullable UUID establishment,
                                    @RequestParam(required = false) @Pattern(regexp = STATUS) @Nullable String status,
                                    @RequestParam(required = false) @Pattern(regexp = KIND) @Nullable String kind,
                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        var slice = repository.decisions(establishment, status, kind, page, size);
        return PageResponse.of(slice.rows().stream().map(DecisionItem::from).toList(), page, size, slice.total());
    }

    @GetMapping("/{id}")
    Decision get(@PathVariable UUID id) {
        return view(find(id), List.of());
    }

    @PostMapping
    @Transactional
    ResponseEntity<Decision> create(@Valid @RequestBody DecisionRequest body, Authentication authentication) {
        var checked = check(body, null);
        UUID id;
        try {
            UUID reportId = body.report() == null ? null : repository.findOrCreateReport(reportInput(body.report()));
            id = repository.insert(input(body, checked.year(), reportId), authentication.getName());
            repository.replacePrograms(id, programs(body));
        } catch (DataIntegrityViolationException e) {
            throw refused(List.of(anomaly("decision", "regle_base", rootMessage(e))));
        }
        announce(id);
        return ResponseEntity.created(URI.create("/api/v1/admin/registry/decisions/" + id)).body(view(find(id), checked.warnings()));
    }

    /** Remplace une décision en brouillon (mêmes contrôles qu'à la création). */
    @PutMapping("/{id}")
    @Transactional
    Decision update(@PathVariable UUID id, @Valid @RequestBody DecisionRequest body) {
        DecisionRow current = find(id);
        if (!"BROUILLON".equals(current.status())) {
            throw problem(HttpStatus.CONFLICT, "DECISION_NOT_EDITABLE",
                    "Une décision validée ne se modifie pas : archivez-la, puis créez-en une nouvelle.");
        }
        var checked = check(body, id);
        try {
            UUID reportId = body.report() == null ? null : repository.findOrCreateReport(reportInput(body.report()));
            repository.update(id, input(body, checked.year(), reportId));
            repository.replacePrograms(id, programs(body));
        } catch (DataIntegrityViolationException e) {
            throw refused(List.of(anomaly("decision", "regle_base", rootMessage(e))));
        }
        announce(id);
        return view(find(id), checked.warnings());
    }

    /** Rend la décision publique : l'avis s'affiche sur la fiche de l'établissement ou de la formation. */
    @PostMapping("/{id}/validate")
    @Transactional
    Decision validate(@PathVariable UUID id, Authentication authentication) {
        find(id);
        try {
            if (!repository.transition(id, "BROUILLON", "VALIDE", authentication.getName())) {
                throw problem(HttpStatus.CONFLICT, "DECISION_NOT_DRAFT", "Seul un brouillon peut être validé.");
            }
        } catch (DataIntegrityViolationException e) {
            throw refused(List.of(anomaly("decision", "regle_base", rootMessage(e))));
        }
        announce(id);
        return view(find(id), List.of());
    }

    /** Retire une décision validée du public en gardant son historique ; le statut courant est recalculé sans elle. */
    @PostMapping("/{id}/archive")
    @Transactional
    Decision archive(@PathVariable UUID id, Authentication authentication) {
        find(id);
        if (!repository.transition(id, "VALIDE", "ARCHIVE", authentication.getName())) {
            throw problem(HttpStatus.CONFLICT, "DECISION_NOT_VALIDATED", "Seule une décision validée peut être archivée.");
        }
        announce(id);
        return view(find(id), List.of());
    }

    @DeleteMapping("/{id}")
    @Transactional
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        find(id);
        if (!repository.deleteDraft(id)) {
            throw problem(HttpStatus.CONFLICT, "DECISION_NOT_DRAFT",
                    "Seul un brouillon se supprime ; archivez une décision validée.");
        }
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------------------------------- contrôles

    private record Checked(int year, List<String> warnings) {
    }

    private Checked check(DecisionRequest b, @Nullable UUID selfId) {
        List<Map<String, String>> anomalies = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);

        // Date et année
        Integer yearFromDate = b.decidedOn() == null ? null : b.decidedOn().getYear();
        if (b.decidedOn() != null && b.decidedOn().isAfter(today)) {
            anomalies.add(anomaly("decidedOn", "date_dans_le_futur",
                    "La date du Conseil scientifique ne peut pas être postérieure à aujourd'hui."));
        }
        if (yearFromDate != null && b.year() != null && !yearFromDate.equals(b.year())) {
            anomalies.add(anomaly("year", "annee_incoherente", "L'année doit être celle de la date du Conseil scientifique."));
        }
        int year = yearFromDate != null ? yearFromDate : b.year() == null ? 0 : b.year();
        if (yearFromDate == null && b.year() == null) {
            anomalies.add(anomaly("year", "annee_requise", "Indiquez la date du Conseil scientifique, ou au moins l'année."));
        }
        if (year != 0 && year < 2014) {
            anomalies.add(anomaly("year", "annee_avant_2014", "L'ANAQ-Sup n'a pas de décision avant 2014."));
        }
        if (year > today.getYear()) {
            anomalies.add(anomaly("year", "date_dans_le_futur", "L'année ne peut pas être postérieure à l'année en cours."));
        }

        // Établissement et formations
        boolean establishmentKnown = repository.establishmentExists(b.establishmentId());
        if (!establishmentKnown) {
            anomalies.add(anomaly("establishmentId", "inconnu", "Cet établissement n'existe pas."));
        }
        List<ProgramAvisRequest> programs = b.programs() == null ? List.of() : b.programs();
        List<UUID> ids = programs.stream().map(ProgramAvisRequest::programId).toList();
        if (ids.size() != new HashSet<>(ids).size()) {
            anomalies.add(anomaly("programs", "programme_en_double", "Une formation apparaît plusieurs fois."));
        }
        Map<UUID, String> titles = repository.programTitles(ids);
        for (UUID id : ids) {
            if (!titles.containsKey(id)) {
                anomalies.add(anomaly("programs", "programme_inconnu", "La formation " + id + " n'existe pas."));
            }
        }
        if (establishmentKnown) {
            Map<UUID, String> inTree = repository.programsInEstablishmentTree(b.establishmentId(), ids);
            titles.forEach((id, title) -> {
                if (!inTree.containsKey(id)) {
                    anomalies.add(anomaly("programs", "programme_hors_etablissement",
                            "« " + title + " » n'appartient pas à cet établissement ni à ses composantes."));
                }
            });
        }
        List<UUID> programReports = programs.stream().map(ProgramAvisRequest::reportId).filter(java.util.Objects::nonNull).toList();
        Set<UUID> knownReports = new HashSet<>(repository.existingReports(programReports));
        programReports.stream().filter(r -> !knownReports.contains(r)).distinct()
                .forEach(r -> anomalies.add(anomaly("programs", "rapport_inconnu", "Le rapport " + r + " n'existe pas.")));

        // Avis : d'ensemble ou formation par formation, jamais les deux ni aucun
        if (KINDS_WITH_PROGRAMS.contains(b.kind()) && programs.isEmpty()) {
            anomalies.add(anomaly("programs", "programmes_requis", "Une accréditation porte sur au moins une formation."));
        }
        if (programs.isEmpty() && b.avis() == null) {
            anomalies.add(anomaly("avis", "avis_manquant", "Donnez l'avis d'ensemble, ou l'avis de chaque formation."));
        }
        if (!programs.isEmpty() && b.avis() != null) {
            anomalies.add(anomaly("avis", "avis_en_double",
                    "L'avis d'ensemble est à omettre quand les formations ont chacune leur avis."));
        }

        // Décision précédente
        if (b.previousId() != null) {
            if (b.previousId().equals(selfId)) {
                anomalies.add(anomaly("previousId", "auto_reference", "Une décision ne peut pas être sa propre précédente."));
            } else {
                repository.decisionEstablishment(b.previousId()).ifPresentOrElse(
                        previous -> {
                            if (!previous.equals(b.establishmentId())) {
                                anomalies.add(anomaly("previousId", "autre_etablissement",
                                        "La décision précédente concerne un autre établissement."));
                            }
                        },
                        () -> anomalies.add(anomaly("previousId", "inconnue", "La décision précédente n'existe pas.")));
            }
        }

        // Rapport
        if (b.report() != null) {
            ReportRequest r = b.report();
            String host = host(r.url());
            if (host == null) {
                anomalies.add(anomaly("report.url", "url_invalide", "Le lien doit commencer par http:// ou https://."));
            } else {
                LocalDate signed = r.signedOn() != null ? r.signedOn() : repository.knownReportSignature(r.url()).orElse(null);
                if (signed != null && b.decidedOn() != null && b.decidedOn().isBefore(signed)) {
                    anomalies.add(anomaly("decidedOn", "date_avant_signature",
                            "La décision (" + b.decidedOn() + ") précède la signature du rapport (" + signed + ")."));
                }
                if (signed != null && b.decidedOn() == null && year != 0 && year < signed.getYear()) {
                    anomalies.add(anomaly("year", "annee_avant_signature",
                            "L'année (" + year + ") précède la signature du rapport (" + signed + ")."));
                }
                if (!host.equals(OFFICIAL_HOST) && !host.endsWith("." + OFFICIAL_HOST)) {
                    warnings.add("Le lien du rapport n'est pas sur " + OFFICIAL_HOST + " : il n'a pas été vérifié.");
                }
            }
            if (ids.size() == 1 && titles.containsKey(ids.get(0))) {   // un rapport collectif n'a pas le titre d'une seule formation
                Set<String> wanted = words(titles.get(ids.get(0)));
                Set<String> found = words(r.title());
                wanted.retainAll(found);
                if (!words(titles.get(ids.get(0))).isEmpty() && wanted.size() < words(titles.get(ids.get(0))).size() / 2.0) {
                    warnings.add("Le titre du rapport ne ressemble pas à l'intitulé de la formation « "
                            + titles.get(ids.get(0)) + " ».");
                }
            }
        }
        if (!anomalies.isEmpty()) {
            throw refused(anomalies);
        }
        return new Checked(year, warnings);
    }

    private Set<String> words(String text) {
        Set<String> words = new HashSet<>();
        for (String w : repository.normalize(text).split("\\s+")) {
            if (w.length() > 3) {
                words.add(w);
            }
        }
        return words;
    }

    private static @Nullable String host(String url) {
        try {
            URI uri = URI.create(url.strip());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) || uri.getHost() == null) {
                return null;
            }
            return uri.getHost().toLowerCase();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------------------------- utilitaires

    private DecisionRow find(UUID id) {
        return repository.decision(id).orElseThrow(() -> new ResourceNotFoundException("Décision", id.toString()));
    }

    private Decision view(DecisionRow d, List<String> warnings) {
        Report report = d.reportId() == null ? null : repository.report(d.reportId()).map(Report::from).orElse(null);
        return new Decision(d.id(), d.kind(), d.establishmentId(), d.establishmentSlug(), d.establishmentName(), d.decidedOn(),
                d.displayYear(), d.validityYears(), d.validUntil(), d.validUntilEstimated(), d.wave(), d.avis(),
                d.effectiveAvis(), d.status(), d.comment(), d.previousId(), report,
                repository.decisionPrograms(d.id()).stream().map(ProgramAvis::from).toList(), d.enteredBy(), d.validatedBy(),
                d.createdAt(), d.updatedAt(), warnings);
    }

    private static DecisionInput input(DecisionRequest b, int year, @Nullable UUID reportId) {
        return new DecisionInput(b.kind(), b.establishmentId(), b.decidedOn(), year, b.validityYears(), b.wave(), b.avis(),
                reportId, b.previousId(), b.comment() == null || b.comment().isBlank() ? null : b.comment().strip());
    }

    private static ReportInput reportInput(ReportRequest r) {
        return new ReportInput(r.kind() == null ? "RAPPORT_EVALUATION_EXTERNE" : r.kind(), r.title().strip(), r.url().strip(),
                r.signedOn(), r.visitedOn(), r.visibility() == null ? "PUBLIC" : r.visibility());
    }

    private static List<ProgramAvisInput> programs(DecisionRequest b) {
        return b.programs() == null ? List.of() : b.programs().stream()
                .map(p -> new ProgramAvisInput(p.programId(), p.avis(), p.reportId(), p.remark())).toList();
    }

    private void announce(UUID decisionId) {
        events.publishEvent(new RegistryChanged(repository.cacheTags(decisionId)));
    }

    private static String rootMessage(Exception e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        String message = t.getMessage() == null ? "Règle de la base de données." : t.getMessage();
        int cut = message.indexOf("\n");
        return cut > 0 ? message.substring(0, cut).replaceFirst("^ERREUR: |^ERROR: ", "") : message;
    }

    private static Map<String, String> anomaly(String field, String rule, String detail) {
        Map<String, String> a = new LinkedHashMap<>();
        a.put("field", field);
        a.put("rule", rule);
        a.put("detail", detail);
        return a;
    }

    private static ErrorResponseException refused(List<? extends Map<String, String>> anomalies) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "La décision n'a pas été enregistrée : " + anomalies.size() + " anomalie(s).");
        problem.setType(Problems.type("admin/validation-refusee"));
        problem.setTitle("Données refusées");
        problem.setProperty("code", "VALIDATION_REFUSEE");
        problem.setProperty("anomalies", anomalies);
        return new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY, problem, null);
    }

    private static ErrorResponseException problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(Problems.type("admin/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle("Requête refusée");
        problem.setProperty("code", code);
        return new ErrorResponseException(status, problem, null);
    }
}
