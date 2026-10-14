package sn.anaqsup.portail.content.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.content.persistence.AdminPublicationRepository.EditablePublication;
import sn.anaqsup.portail.medias.MediaKeys;

/** Corps des requêtes et réponses du back-office pour les ressources documentaires. */
final class AdminPublicationDtos {

    static final String KINDS = "RAPPORT_ANNUEL|REFERENTIEL|GUIDE|TEXTE_REGLEMENTAIRE|DISCOURS|ETUDE|AUTRE";

    private AdminPublicationDtos() {
    }

    record Publication(UUID id, String slug, String title, @Nullable String summary, String kind, LocalDate publishedOn,
                       boolean published, @Nullable String fileKey, @Nullable String fileUrl, @Nullable String fileName,
                       @Nullable Integer fileSizeBytes, @Nullable String coverKey, @Nullable String coverUrl,
                       boolean coverGenerated, @Nullable String titleEn, @Nullable String summaryEn, boolean englishAuto,
                       Instant updatedAt) {
        static Publication from(EditablePublication row) {
            String fileKey = row.fileKey() == null ? null : row.fileKey().toString();
            String coverKey = row.coverKey() == null ? null : row.coverKey().toString();
            return new Publication(row.id(), row.slug(), row.title(), row.summary(), row.kind(), row.publishedOn(),
                    row.published(), fileKey, fileKey == null ? row.fileUrl() : MediaKeys.publicUrl(fileKey), row.fileName(),
                    row.fileSizeBytes(), coverKey, MediaKeys.publicUrl(coverKey), row.coverGenerated(), row.titleEn(),
                    row.summaryEn(), row.enAuto(), row.updatedAt());
        }
    }

    record PublicationRequest(@NotBlank @Size(max = 300) String title,
                              @Nullable @Size(max = 1000) String summary,
                              @NotBlank @Pattern(regexp = KINDS) String kind,
                              @NotNull LocalDate publishedOn,
                              boolean published,
                              @Nullable @Size(max = 64) String fileKey,
                              @Nullable @Size(max = 64) String coverKey,
                              @Nullable @Size(max = 300) String titleEn,
                              @Nullable @Size(max = 1000) String summaryEn) {
    }
}
