package sn.anaqsup.portail.content.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.content.persistence.ContentRepository;
import sn.anaqsup.portail.content.persistence.ContentRepository.EventRow;
import sn.anaqsup.portail.content.persistence.ContentRepository.NewsRow;
import sn.anaqsup.portail.content.persistence.ContentRepository.PageRow;
import sn.anaqsup.portail.content.persistence.ContentRepository.PartnerRow;
import sn.anaqsup.portail.content.persistence.ContentRepository.PublicationRow;
import sn.anaqsup.portail.medias.MediaKeys;

/** DTO du contrat public v1 pour le contenu institutionnel. */
final class ContentResponses {

    private ContentResponses() {
    }

    record Category(String slug, String label) {
    }

    record NewsSummary(String slug, String lang, String title, @Nullable String summary, Category category,
                       @Nullable String coverImageUrl, boolean featured, Instant publishedAt) {
        static NewsSummary from(NewsRow row) {
            return new NewsSummary(row.slug(), row.lang(), row.title(), row.summary(),
                    new Category(row.categorySlug(), row.categoryLabel()), MediaKeys.publicUrl(row.coverImageKey()), row.featured(),
                    row.publishedAt());
        }
    }

    record NewsLink(String slug, String lang, String title) {
        static @Nullable NewsLink from(java.util.Optional<ContentRepository.NewsLink> row) {
            return row.map(r -> new NewsLink(r.slug(), r.lang(), r.title())).orElse(null);
        }
    }

    /** {@code previous} : article publié juste avant (plus ancien) ; {@code next} : juste après (plus récent). */
    record NewsItem(String slug, String lang, String title, @Nullable String summary, Category category,
                    @Nullable String coverImageUrl, boolean featured, Instant publishedAt, Instant updatedAt, String bodyHtml,
                    List<Object> attachments, @Nullable NewsLink previous, @Nullable NewsLink next) {
        static NewsItem from(NewsRow row, @Nullable NewsLink previous, @Nullable NewsLink next) {
            return new NewsItem(row.slug(), row.lang(), row.title(), row.summary(),
                    new Category(row.categorySlug(), row.categoryLabel()), MediaKeys.publicUrl(row.coverImageKey()), row.featured(),
                    row.publishedAt(), row.updatedAt(), row.bodyHtml() == null ? "" : row.bodyHtml(), List.of(), previous, next);
        }
    }

    record EventSummary(String slug, String lang, String title, @Nullable String summary, @Nullable String location,
                        Instant startsAt, @Nullable Instant endsAt, @Nullable String coverImageUrl) {
        static EventSummary from(EventRow row) {
            return new EventSummary(row.slug(), row.lang(), row.title(), row.summary(), row.location(), row.startsAt(), row.endsAt(), null);
        }
    }

    record Event(String slug, String lang, String title, @Nullable String summary, @Nullable String location, Instant startsAt,
                 @Nullable Instant endsAt, @Nullable String coverImageUrl, String bodyHtml) {
        static Event from(EventRow row) {
            return new Event(row.slug(), row.lang(), row.title(), row.summary(), row.location(), row.startsAt(), row.endsAt(), null,
                    row.bodyHtml() == null ? "" : row.bodyHtml());
        }
    }

    /**
     * Document : {@code fileUrl} pointe vers le PDF téléversé (/media/…) ou, à défaut, vers un lien externe ;
     * {@code fileType} et {@code fileSizeBytes} ne sont connus que pour un fichier téléversé (ou un lien en .pdf).
     */
    record Publication(String slug, String lang, String title, String kind, @Nullable String summary, LocalDate publishedOn,
                       @Nullable String fileUrl, @Nullable String fileType, @Nullable Integer fileSizeBytes,
                       @Nullable String coverImageUrl) {
        static Publication from(PublicationRow row) {
            boolean uploaded = row.fileKey() != null;
            String fileUrl = uploaded ? MediaKeys.publicUrl(row.fileKey().toString()) : row.fileUrl();
            return new Publication(row.slug(), row.lang(), row.title(), row.kind(), row.summary(), row.publishedOn(), fileUrl,
                    fileType(uploaded ? row.fileContentType() : null, fileUrl), uploaded ? row.fileSizeBytes() : null,
                    row.coverKey() == null ? null : MediaKeys.publicUrl(row.coverKey().toString()));
        }

        private static @Nullable String fileType(@Nullable String contentType, @Nullable String url) {
            if ("application/pdf".equals(contentType) || (url != null && url.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf"))) {
                return "PDF";
            }
            return null;
        }
    }

    record InstitutionalPage(String slug, String lang, String title, @Nullable String summary, String bodyHtml,
                             @Nullable String imageUrl, @Nullable String imageName, @Nullable String imageRole,
                             Instant updatedAt) {
        static InstitutionalPage from(PageRow row) {
            return new InstitutionalPage(row.slug(), row.lang(), row.title(), row.summary(), row.bodyHtml(),
                    MediaKeys.publicUrl(row.imageKey()), row.imageName(), row.imageRole(), row.updatedAt());
        }
    }

    record Partner(String name, @Nullable String url, @Nullable String logoUrl) {
        static Partner from(PartnerRow row) {
            return new Partner(row.name(), row.url(), MediaKeys.publicUrl(row.logoKey()));
        }
    }
}
