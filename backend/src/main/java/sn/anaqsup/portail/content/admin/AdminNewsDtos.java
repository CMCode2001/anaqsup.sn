package sn.anaqsup.portail.content.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.content.persistence.AdminNewsRepository.CategoryRow;
import sn.anaqsup.portail.content.persistence.AdminNewsRepository.EditableNews;
import sn.anaqsup.portail.medias.MediaKeys;

/** Corps des requêtes et réponses du back-office pour les actualités et leurs rubriques. */
final class AdminNewsDtos {

    /** Rubriques liées à une page du menu (Actualités ▾) : ni supprimables, ni renommables en adresse. */
    static final List<String> MENU_CATEGORIES = List.of("cooperation-et-partenariats", "communiques");

    private AdminNewsDtos() {
    }

    record News(UUID id, String slug, String title, @Nullable String summary, String bodyHtml, String category,
                String categoryLabel, @Nullable String coverKey, @Nullable String coverUrl, boolean published,
                @Nullable Instant publishedAt, boolean featured, @Nullable String titleEn, @Nullable String summaryEn,
                @Nullable String bodyHtmlEn, boolean englishAuto, boolean legacy, Instant updatedAt, long likes,
                long dislikes) {
        static News from(EditableNews row) {
            return new News(row.id(), row.slug(), row.title(), row.summary(), row.bodyHtml(), row.categorySlug(),
                    row.categoryLabel(), row.coverKey(), MediaKeys.publicUrl(row.coverKey()), row.published(),
                    row.publishedAt(), row.featured(), row.titleEn(), row.summaryEn(), row.bodyHtmlEn(), row.enAuto(),
                    row.legacy(), row.updatedAt(), row.likes(), row.dislikes());
        }
    }

    record NewsRequest(@NotBlank @Size(max = 300) String title,
                       @Nullable @Size(max = 1000) String summary,
                       @NotNull @Size(max = 200_000) String bodyHtml,
                       @NotBlank @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") String category,
                       @Nullable @Size(max = 200) String coverKey,
                       boolean published,
                       @Nullable Instant publishedAt,
                       boolean featured,
                       @Nullable @Size(max = 300) String titleEn,
                       @Nullable @Size(max = 1000) String summaryEn,
                       @Nullable @Size(max = 200_000) String bodyHtmlEn) {
    }

    record Category(UUID id, String slug, String label, @Nullable String labelEn, int position, long articles,
                    boolean menu) {
        static Category from(CategoryRow row) {
            return new Category(row.id(), row.slug(), row.label(), row.labelEn(), row.position(), row.articles(),
                    MENU_CATEGORIES.contains(row.slug()));
        }
    }

    record CategoryRequest(@NotBlank @Size(max = 80) String label, @Nullable @Size(max = 80) String labelEn) {
    }

    record CategoryOrder(@NotNull List<UUID> ids) {
    }
}
