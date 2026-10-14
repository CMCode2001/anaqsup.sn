package sn.anaqsup.portail.content.admin;

import static sn.anaqsup.portail.content.admin.AdminContentService.blankToNull;
import static sn.anaqsup.portail.content.admin.AdminContentService.invalid;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.content.admin.AdminPublicationDtos.PublicationRequest;
import sn.anaqsup.portail.content.persistence.AdminPublicationRepository;
import sn.anaqsup.portail.content.persistence.AdminPublicationRepository.EditablePublication;
import sn.anaqsup.portail.content.persistence.AdminPublicationRepository.PublicationChanges;
import sn.anaqsup.portail.medias.MediaKeys;
import sn.anaqsup.portail.medias.MediaStorage;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;
import sn.anaqsup.portail.shared.text.Slugs;

/**
 * Ressources documentaires du back-office, publiées directement (décision ANAQ-Sup).
 * Version anglaise : saisie ou corrigée par l'éditeur, sinon traduite automatiquement après l'enregistrement.
 */
@Service
class AdminPublicationService {

    private static final List<String> TAGS = List.of("publications");

    private final AdminPublicationRepository repository;
    private final MediaStorage media;
    private final ApplicationEventPublisher events;

    AdminPublicationService(AdminPublicationRepository repository, MediaStorage media, ApplicationEventPublisher events) {
        this.repository = repository;
        this.media = media;
        this.events = events;
    }

    @Transactional
    EditablePublication create(PublicationRequest body, String author) {
        UUID id = repository.insert(uniqueSlug(body.title()), changes(body, null), author);
        events.publishEvent(new ContentChanged(TAGS));
        return repository.find(id).orElseThrow();
    }

    @Transactional
    EditablePublication update(UUID id, PublicationRequest body, String author) {
        EditablePublication current = find(id);
        repository.update(id, changes(body, current), author);
        events.publishEvent(new ContentChanged(TAGS));
        return repository.find(id).orElseThrow();
    }

    @Transactional
    void delete(UUID id) {
        find(id);
        repository.delete(id);
        events.publishEvent(new ContentChanged(TAGS));
    }

    EditablePublication find(UUID id) {
        return repository.find(id).orElseThrow(() -> new ResourceNotFoundException("Document", id.toString()));
    }

    // ------------------------------------------------------------------------------------------ règles

    private PublicationChanges changes(PublicationRequest body, @Nullable EditablePublication current) {
        UUID fileKey = checkedMedia(body.fileKey(), MediaStorage.Media::isPdf, "UNKNOWN_DOCUMENT",
                "Document introuvable : téléversez le PDF à nouveau.");
        UUID coverKey = checkedMedia(body.coverKey(), MediaStorage.Media::isImage, "UNKNOWN_MEDIA",
                "Vignette introuvable : téléversez l'image à nouveau.");
        boolean hasExternalFile = current != null && current.fileUrl() != null;
        if (body.published() && fileKey == null && !hasExternalFile) {
            throw invalid("DOCUMENT_REQUIRED", "Joignez le PDF avant de publier le document.");
        }

        String titleEn = blankToNull(body.titleEn());
        String summaryEn = titleEn == null ? null : blankToNull(body.summaryEn());
        // Traduction automatique laissée telle quelle : elle reste « automatique » (refaite si le français change).
        // Toute retouche en fait une traduction relue, que la traduction automatique n'écrasera plus.
        boolean enAuto = titleEn != null && current != null && current.enAuto()
                && Objects.equals(titleEn, current.titleEn()) && Objects.equals(summaryEn, current.summaryEn());

        return new PublicationChanges(body.title().strip(), blankToNull(body.summary()), body.kind(), body.publishedOn(),
                body.published(), fileKey, coverKey, titleEn, summaryEn, enAuto);
    }

    private @Nullable UUID checkedMedia(@Nullable String key, Predicate<MediaStorage.Media> accepted, String code, String detail) {
        String value = blankToNull(key);
        if (value == null) {
            return null;
        }
        if (!MediaKeys.isUuid(value)) {
            throw invalid(code, detail);
        }
        UUID id = UUID.fromString(value);
        return media.find(id).filter(accepted).map(MediaStorage.Media::id).orElseThrow(() -> invalid(code, detail));
    }

    /** Adresse stable tirée du titre : « Référentiel d'évaluation (2026) » → referentiel-d-evaluation-2026. */
    private String uniqueSlug(String title) {
        return Slugs.unique(Slugs.from(title), repository::slugExists);
    }
}
