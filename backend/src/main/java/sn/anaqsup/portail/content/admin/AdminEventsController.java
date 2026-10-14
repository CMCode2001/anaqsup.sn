package sn.anaqsup.portail.content.admin;

import static sn.anaqsup.portail.content.admin.AdminContentService.blankToNull;
import static sn.anaqsup.portail.content.admin.AdminContentService.invalid;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.content.persistence.AdminEventRepository;
import sn.anaqsup.portail.content.persistence.AdminEventRepository.EditableEvent;
import sn.anaqsup.portail.content.persistence.AdminEventRepository.EventChanges;
import sn.anaqsup.portail.shared.web.PageResponse;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;
import sn.anaqsup.portail.shared.text.Slugs;

/**
 * Back-office : événements (permission EVENEMENTS), publiés directement. Version anglaise : saisie ou corrigée,
 * sinon traduite automatiquement après l'enregistrement.
 */
@RestController
@RequestMapping("/api/v1/admin/events")
class AdminEventsController {

    record Event(UUID id, String slug, String title, @Nullable String summary, String bodyHtml, @Nullable String location,
                 Instant startsAt, @Nullable Instant endsAt, boolean published, @Nullable String titleEn,
                 @Nullable String summaryEn, @Nullable String bodyHtmlEn, boolean englishAuto, Instant updatedAt) {
        static Event from(EditableEvent e) {
            return new Event(e.id(), e.slug(), e.title(), e.summary(), e.bodyHtml(), e.location(), e.startsAt(), e.endsAt(),
                    e.published(), e.titleEn(), e.summaryEn(), e.bodyHtmlEn(), e.enAuto(), e.updatedAt());
        }
    }

    record EventRequest(@NotBlank @Size(max = 300) String title,
                        @Nullable @Size(max = 1000) String summary,
                        @NotNull @Size(max = 200_000) String bodyHtml,
                        @Nullable @Size(max = 200) String location,
                        @NotNull Instant startsAt,
                        @Nullable Instant endsAt,
                        boolean published,
                        @Nullable @Size(max = 300) String titleEn,
                        @Nullable @Size(max = 1000) String summaryEn,
                        @Nullable @Size(max = 200_000) String bodyHtmlEn) {
    }

    private final AdminEventRepository repository;
    private final ApplicationEventPublisher events;

    AdminEventsController(AdminEventRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @GetMapping
    PageResponse<Event> list(@RequestParam(required = false) @Size(max = 100) @Nullable String q,
                             @RequestParam(required = false) @Nullable Boolean upcoming,
                             @RequestParam(defaultValue = "0") @Min(0) int page,
                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var slice = repository.list(q, upcoming, page, size);
        return PageResponse.of(slice.rows().stream().map(Event::from).toList(), page, size, slice.total());
    }

    @GetMapping("/{id}")
    Event get(@PathVariable UUID id) {
        return Event.from(find(id));
    }

    @PostMapping
    @Transactional
    ResponseEntity<Event> create(@Valid @RequestBody EventRequest body, Authentication authentication) {
        String slug = Slugs.unique(Slugs.from(body.title()), repository::slugTaken);
        UUID id = repository.insert(slug, changes(body, null), authentication.getName());
        announce(slug);
        var created = Event.from(repository.find(id).orElseThrow());
        return ResponseEntity.created(URI.create("/api/v1/admin/events/" + id)).body(created);
    }

    @PutMapping("/{id}")
    @Transactional
    Event update(@PathVariable UUID id, @Valid @RequestBody EventRequest body, Authentication authentication) {
        EditableEvent current = find(id);
        repository.update(id, changes(body, current), authentication.getName());
        announce(current.slug());
        return Event.from(repository.find(id).orElseThrow());
    }

    @DeleteMapping("/{id}")
    @Transactional
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        EditableEvent current = find(id);
        repository.delete(id);
        announce(current.slug());
        return ResponseEntity.noContent().build();
    }

    private EditableEvent find(UUID id) {
        return repository.find(id).orElseThrow(() -> new ResourceNotFoundException("Événement", id.toString()));
    }

    private static EventChanges changes(EventRequest body, @Nullable EditableEvent current) {
        if (body.endsAt() != null && body.endsAt().isBefore(body.startsAt())) {
            throw invalid("END_BEFORE_START", "La fin de l'événement doit suivre son début.");
        }
        String bodyHtml = HtmlSanitizer.clean(body.bodyHtml());
        String titleEn = blankToNull(body.titleEn());
        String bodyHtmlEn = titleEn == null ? null
                : blankToNull(body.bodyHtmlEn()) == null ? "" : HtmlSanitizer.clean(body.bodyHtmlEn());
        String summaryEn = titleEn == null ? null : blankToNull(body.summaryEn());
        // Traduction automatique renvoyée telle quelle : elle reste automatique ; retouchée, elle devient relue.
        boolean enAuto = titleEn != null && current != null && current.enAuto() && Objects.equals(titleEn, current.titleEn())
                && Objects.equals(summaryEn, current.summaryEn()) && Objects.equals(bodyHtmlEn, current.bodyHtmlEn());
        return new EventChanges(body.title().strip(), blankToNull(body.summary()), bodyHtml, blankToNull(body.location()),
                body.startsAt(), body.endsAt(), body.published(), titleEn, summaryEn, bodyHtmlEn, enAuto);
    }

    private void announce(String slug) {
        events.publishEvent(new ContentChanged(List.of("events", "event:" + slug)));
    }
}
