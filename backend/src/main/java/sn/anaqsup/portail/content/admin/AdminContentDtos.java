package sn.anaqsup.portail.content.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.content.persistence.AdminContentRepository.EditablePage;
import sn.anaqsup.portail.content.persistence.AdminContentRepository.PartnerRow;
import sn.anaqsup.portail.medias.MediaKeys;

/** Corps des requêtes et réponses du back-office pour les pages et les partenaires. */
final class AdminContentDtos {

    private AdminContentDtos() {
    }

    record PageSummary(String slug, String title, boolean hasEnglish, boolean hasImage, Instant updatedAt) {
        static PageSummary from(EditablePage page) {
            return new PageSummary(page.slug(), page.title(), page.titleEn() != null, page.imageKey() != null, page.updatedAt());
        }
    }

    record Page(String slug, String title, @Nullable String summary, String bodyHtml,
                @Nullable String titleEn, @Nullable String summaryEn, @Nullable String bodyHtmlEn,
                @Nullable String imageKey, @Nullable String imageUrl, @Nullable String imageName,
                @Nullable String imageRole, @Nullable String imageRoleEn, boolean englishAuto, @Nullable Instant updatedAt) {
        static Page from(EditablePage page) {
            return new Page(page.slug(), page.title(), page.summary(), page.bodyHtml(), page.titleEn(), page.summaryEn(),
                    page.bodyHtmlEn(), page.imageKey(), MediaKeys.publicUrl(page.imageKey()), page.imageName(),
                    page.imageRole(), page.imageRoleEn(), page.enAuto(), page.updatedAt());
        }
    }

    record PageRequest(@NotBlank @Size(max = 200) String title,
                       @Nullable @Size(max = 1000) String summary,
                       @NotBlank @Size(max = 200_000) String bodyHtml,
                       @Nullable @Size(max = 200) String titleEn,
                       @Nullable @Size(max = 1000) String summaryEn,
                       @Nullable @Size(max = 200_000) String bodyHtmlEn,
                       @Nullable @Size(max = 200) String imageKey,
                       @Nullable @Size(max = 120) String imageName,
                       @Nullable @Size(max = 200) String imageRole,
                       @Nullable @Size(max = 200) String imageRoleEn) {
    }

    record Partner(UUID id, String name, @Nullable String url, @Nullable String logoKey, @Nullable String logoUrl,
                   int position, boolean enabled) {
        static Partner from(PartnerRow row) {
            return new Partner(row.id(), row.name(), row.url(), row.logoKey(), MediaKeys.publicUrl(row.logoKey()),
                    row.position(), row.enabled());
        }
    }

    record PartnerRequest(@NotBlank @Size(max = 120) String name,
                          @Nullable @Size(max = 500) String url,
                          @Nullable @Size(max = 200) String logoKey,
                          boolean enabled) {
    }

    record PartnerOrder(@NotNull List<UUID> ids) {
    }
}
