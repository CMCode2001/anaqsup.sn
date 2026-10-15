package sn.anaqsup.portail.referentiel.web;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.PublicEntry;
import sn.anaqsup.portail.referentiel.persistence.DecisionRepository.PublicReport;
import sn.anaqsup.portail.referentiel.persistence.RegistryRepository.ComponentRow;
import sn.anaqsup.portail.referentiel.persistence.RegistryRepository.EstablishmentRow;
import sn.anaqsup.portail.referentiel.persistence.RegistryRepository.ProgramRow;

/**
 * DTO du contrat public v1 (contracts/openapi) — jamais les lignes SQL directement.
 * Les champs {@code habilitationAvis}, {@code habilitationAvisYear}, {@code accreditationAvis} et
 * {@code accreditationAvisYear} reflètent la décision validée la plus récente : le portail les lit comme avant.
 */
final class RegistryResponses {

    private RegistryResponses() {
    }

    record EstablishmentSummary(String slug, String name, @Nullable String acronym, String kind, @Nullable String city,
                                @Nullable String habilitationAvis, @Nullable Integer habilitationAvisYear,
                                String nature, boolean provisional, @Nullable EstablishmentRef parent) {
        static EstablishmentSummary from(EstablishmentRow row) {
            return new EstablishmentSummary(row.slug(), row.name(), row.acronym(), row.kind(), row.city(),
                    row.avis(), row.avisYear(), row.nature(), row.provisional(), EstablishmentRef.parentOf(row));
        }
    }

    /**
     * Fiche d'un établissement. {@code programCount} compte ses formations et celles de ses composantes ;
     * la liste complète est sur {@code /establishments/{slug}/programs}.
     */
    record Establishment(String slug, String name, @Nullable String acronym, String kind, @Nullable String city,
                         @Nullable String habilitationAvis, @Nullable Integer habilitationAvisYear,
                         @Nullable String region, @Nullable String website, long programCount,
                         String nature, boolean provisional, @Nullable EstablishmentRef parent, List<Component> components,
                         @Nullable DecisionEntry currentHabilitation) {
        static Establishment from(EstablishmentRow row, long programCount, List<ComponentRow> components,
                                  @Nullable PublicEntry current) {
            return new Establishment(row.slug(), row.name(), row.acronym(), row.kind(), row.city(), row.avis(),
                    row.avisYear(), row.region(), row.website(), programCount, row.nature(), row.provisional(),
                    EstablishmentRef.parentOf(row), components.stream().map(Component::from).toList(),
                    current == null ? null : DecisionEntry.from(current));
        }
    }

    record EstablishmentRef(String slug, String name) {
        static @Nullable EstablishmentRef parentOf(EstablishmentRow row) {
            return row.parentSlug() == null ? null : new EstablishmentRef(row.parentSlug(), row.parentName());
        }
    }

    record Component(String slug, String name, @Nullable String acronym, String nature, long programCount) {
        static Component from(ComponentRow row) {
            return new Component(row.slug(), row.name(), row.acronym(), row.nature(), row.programCount());
        }
    }

    record ProgramSummary(String slug, String title, String level, @Nullable String domain,
                          EstablishmentRef establishment, @Nullable String accreditationAvis,
                          @Nullable Integer accreditationAvisYear, @Nullable String optionParcours, boolean provisional) {
        static ProgramSummary from(ProgramRow row) {
            return new ProgramSummary(row.slug(), row.title(), row.level(), row.domain(),
                    new EstablishmentRef(row.establishmentSlug(), row.establishmentName()), row.avis(), row.avisYear(),
                    row.optionParcours(), row.provisional());
        }
    }

    /** Fiche d'une formation : son résumé et l'historique de ses décisions (la plus récente en premier). */
    record Program(String slug, String title, String level, @Nullable String domain, EstablishmentRef establishment,
                   @Nullable String accreditationAvis, @Nullable Integer accreditationAvisYear,
                   @Nullable String optionParcours, boolean provisional, List<DecisionEntry> decisions) {
        static Program from(ProgramRow row, List<PublicEntry> history) {
            return new Program(row.slug(), row.title(), row.level(), row.domain(),
                    new EstablishmentRef(row.establishmentSlug(), row.establishmentName()), row.avis(), row.avisYear(),
                    row.optionParcours(), row.provisional(), history.stream().map(DecisionEntry::from).toList());
        }
    }

    /**
     * Une décision de l'historique. {@code avis} : celui de la formation, ou l'avis d'ensemble de l'établissement
     * (« MIXTE » quand les formations n'ont pas toutes le même). {@code validUntilEstimated} : la date exacte du
     * Conseil scientifique n'est pas connue, la fin de validité est estimée à partir de l'année.
     */
    record DecisionEntry(String id, String kind, int year, @Nullable LocalDate decidedOn, int validityYears,
                         LocalDate validUntil, boolean validUntilEstimated, boolean inForce, @Nullable String avis,
                         @Nullable Integer wave, @Nullable Report report) {
        static DecisionEntry from(PublicEntry e) {
            return new DecisionEntry(e.id().toString(), e.kind(), e.year(), e.decidedOn(), e.validityYears(), e.validUntil(),
                    e.validUntilEstimated(), e.inForce(), e.avis(), e.wave(),
                    e.report() == null ? null : Report.from(e.report()));
        }
    }

    record Report(String title, String url, @Nullable LocalDate signedOn, @Nullable Boolean linkOk) {
        static Report from(PublicReport r) {
            return new Report(r.title(), r.url(), r.signedOn(), r.linkOk());
        }
    }
}
