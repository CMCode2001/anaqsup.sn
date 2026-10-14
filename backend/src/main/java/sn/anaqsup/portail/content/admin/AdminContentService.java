package sn.anaqsup.portail.content.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.content.admin.AdminContentDtos.PageRequest;
import sn.anaqsup.portail.content.admin.AdminContentDtos.PartnerRequest;
import sn.anaqsup.portail.content.persistence.AdminContentRepository;
import sn.anaqsup.portail.content.persistence.AdminContentRepository.EditablePage;
import sn.anaqsup.portail.content.persistence.AdminContentRepository.PartnerRow;
import sn.anaqsup.portail.medias.MediaKeys;
import sn.anaqsup.portail.shared.web.Problems;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Modifications du back-office, publiées directement (décision ANAQ-Sup). Chaque modification annonce
 * {@link ContentChanged} : le portail vide son cache et affiche aussitôt la nouvelle version.
 */
@Service
class AdminContentService {

    private final AdminContentRepository repository;
    private final ApplicationEventPublisher events;

    AdminContentService(AdminContentRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    // ------------------------------------------------------------------------------------------ pages

    @Transactional
    EditablePage updatePage(String slug, PageRequest body, String author) {
        EditablePage current = repository.page(slug).orElseThrow(() -> new ResourceNotFoundException("Page", slug));
        String bodyHtml = HtmlSanitizer.clean(body.bodyHtml());
        String bodyHtmlEn = blankToNull(body.bodyHtmlEn()) == null ? null : HtmlSanitizer.clean(body.bodyHtmlEn());
        String titleEn = blankToNull(body.titleEn());
        if ((titleEn == null) != (blankToNull(bodyHtmlEn) == null)) {
            throw invalid("ENGLISH_INCOMPLETE", "La version anglaise doit avoir un titre ET un texte (ou rester vide).");
        }
        if (bodyHtml.isBlank()) {
            throw invalid("EMPTY_BODY", "Le texte de la page est vide.");
        }
        String imageKey = checkedMedia(body.imageKey());
        String summaryEn = titleEn == null ? null : blankToNull(body.summaryEn());
        bodyHtmlEn = titleEn == null ? null : bodyHtmlEn;
        String roleEn = blankToNull(body.imageRoleEn());
        // Traduction automatique renvoyée telle quelle : elle reste automatique (refaite si le français change).
        // Toute retouche en fait une traduction relue, que la traduction automatique n'écrasera plus.
        boolean enAuto = titleEn != null && current.enAuto() && Objects.equals(titleEn, current.titleEn())
                && Objects.equals(summaryEn, current.summaryEn()) && Objects.equals(bodyHtmlEn, current.bodyHtmlEn())
                && Objects.equals(roleEn, current.imageRoleEn());
        repository.updatePage(new EditablePage(slug, body.title().strip(), blankToNull(body.summary()), bodyHtml,
                titleEn, summaryEn, bodyHtmlEn, imageKey, blankToNull(body.imageName()), blankToNull(body.imageRole()),
                roleEn, enAuto, null), author);
        events.publishEvent(new ContentChanged(List.of("page:" + slug)));
        return repository.page(slug).orElseThrow();
    }

    // ------------------------------------------------------------------------------------- partenaires

    @Transactional
    PartnerRow createPartner(PartnerRequest body) {
        UUID id = repository.insertPartner(body.name(), checkedUrl(body.url()), checkedMedia(body.logoKey()), body.enabled());
        partnersChanged();
        return repository.partner(id).orElseThrow();
    }

    @Transactional
    PartnerRow updatePartner(UUID id, PartnerRequest body) {
        repository.partner(id).orElseThrow(() -> new ResourceNotFoundException("Partenaire", id.toString()));
        repository.updatePartner(id, body.name(), checkedUrl(body.url()), checkedMedia(body.logoKey()), body.enabled());
        partnersChanged();
        return repository.partner(id).orElseThrow();
    }

    @Transactional
    void deletePartner(UUID id) {
        repository.partner(id).orElseThrow(() -> new ResourceNotFoundException("Partenaire", id.toString()));
        repository.deletePartner(id);
        partnersChanged();
    }

    @Transactional
    List<PartnerRow> reorderPartners(List<UUID> ids) {
        var known = repository.partners().stream().map(PartnerRow::id).toList();
        if (ids.size() != known.size() || !new HashSet<>(ids).equals(new HashSet<>(known))) {
            throw invalid("INCOMPLETE_ORDER", "L'ordre doit contenir chaque partenaire une seule fois.");
        }
        repository.reorderPartners(ids);
        partnersChanged();
        return repository.partners();
    }

    private void partnersChanged() {
        events.publishEvent(new ContentChanged(List.of("partners")));
    }

    // ------------------------------------------------------------------------------------------ règles

    private static @Nullable String checkedMedia(@Nullable String key) {
        String value = blankToNull(key);
        if (value != null && MediaKeys.publicUrl(value) == null) {
            throw invalid("UNKNOWN_MEDIA", "Image inconnue : téléversez-la à nouveau.");
        }
        return value;
    }

    private static @Nullable String checkedUrl(@Nullable String url) {
        String value = blankToNull(url);
        if (value != null && !value.matches("^https?://\\S+$")) {
            throw invalid("INVALID_URL", "L'adresse du site doit commencer par https://");
        }
        return value;
    }

    static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    static ErrorResponseException invalid(String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(Problems.type("admin/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle("Requête refusée");
        problem.setProperty("code", code);
        return new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
    }
}
