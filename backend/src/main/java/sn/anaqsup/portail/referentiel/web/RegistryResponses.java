package sn.anaqsup.portail.referentiel.web;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.referentiel.persistence.RegistryRepository.EstablishmentRow;
import sn.anaqsup.portail.referentiel.persistence.RegistryRepository.ProgramRow;

/** DTO du contrat public v1 (contracts/openapi) — jamais les lignes SQL directement. */
final class RegistryResponses {

    private RegistryResponses() {
    }

    record EstablishmentSummary(String slug, String name, @Nullable String acronym, String kind, @Nullable String city,
                                @Nullable String habilitationAvis, @Nullable Integer habilitationAvisYear) {
        static EstablishmentSummary from(EstablishmentRow row) {
            return new EstablishmentSummary(row.slug(), row.name(), row.acronym(), row.kind(), row.city(),
                    row.avis(), row.avisYear());
        }
    }

    record Establishment(String slug, String name, @Nullable String acronym, String kind, @Nullable String city,
                         @Nullable String habilitationAvis, @Nullable Integer habilitationAvisYear,
                         @Nullable String region, @Nullable String website, long programCount) {
        static Establishment from(EstablishmentRow row, long programCount) {
            return new Establishment(row.slug(), row.name(), row.acronym(), row.kind(), row.city(), row.avis(),
                    row.avisYear(), row.region(), row.website(), programCount);
        }
    }

    record EstablishmentRef(String slug, String name) {
    }

    record ProgramSummary(String slug, String title, String level, @Nullable String domain,
                          EstablishmentRef establishment, @Nullable String accreditationAvis,
                          @Nullable Integer accreditationAvisYear) {
        static ProgramSummary from(ProgramRow row) {
            return new ProgramSummary(row.slug(), row.title(), row.level(), row.domain(),
                    new EstablishmentRef(row.establishmentSlug(), row.establishmentName()), row.avis(), row.avisYear());
        }
    }
}
