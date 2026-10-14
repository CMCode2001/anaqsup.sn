package sn.anaqsup.portail.content.admin;

import static sn.anaqsup.portail.content.admin.AdminContentService.blankToNull;
import static sn.anaqsup.portail.content.admin.AdminContentService.invalid;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.content.admin.AdminNewsDtos.CategoryRequest;
import sn.anaqsup.portail.content.admin.AdminNewsDtos.NewsRequest;
import sn.anaqsup.portail.content.persistence.AdminNewsRepository;
import sn.anaqsup.portail.content.persistence.AdminNewsRepository.CategoryRow;
import sn.anaqsup.portail.content.persistence.AdminNewsRepository.EditableNews;
import sn.anaqsup.portail.content.persistence.AdminNewsRepository.NewsChanges;
import sn.anaqsup.portail.medias.MediaKeys;
import sn.anaqsup.portail.medias.MediaStorage;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;
import sn.anaqsup.portail.shared.text.Slugs;

/**
 * Actualités du back-office, publiées directement (décision ANAQ-Sup). « À la une » : un seul article épinglé
 * (l'ancien est désépinglé). Version anglaise : saisie ou corrigée, sinon traduite automatiquement.
 */
@Service
class AdminNewsService {

    private final AdminNewsRepository repository;
    private final MediaStorage media;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    AdminNewsService(AdminNewsRepository repository, MediaStorage media, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.media = media;
        this.events = events;
        this.clock = clock;
    }

    EditableNews find(UUID id) {
        return repository.find(id).orElseThrow(() -> new ResourceNotFoundException("Actualité", id.toString()));
    }

    @Transactional
    EditableNews create(NewsRequest body, String author) {
        NewsChanges changes = changes(body, null);
        if (changes.featured()) {
            repository.unpinAllExcept(null);
        }
        String slug = Slugs.unique(Slugs.from(body.title()), repository::slugTaken);
        UUID id = repository.insert(slug, changes, author);
        announce(slug);
        return repository.find(id).orElseThrow();
    }

    @Transactional
    EditableNews update(UUID id, NewsRequest body, String author) {
        EditableNews current = find(id);
        NewsChanges changes = changes(body, current);
        if (changes.featured()) {
            repository.unpinAllExcept(id);
        }
        repository.update(id, changes, author);
        announce(current.slug());
        return repository.find(id).orElseThrow();
    }

    @Transactional
    void delete(UUID id) {
        EditableNews current = find(id);
        repository.delete(id);
        announce(current.slug());
    }

    private NewsChanges changes(NewsRequest body, @Nullable EditableNews current) {
        UUID categoryId = repository.categoryId(body.category())
                .orElseThrow(() -> invalid("UNKNOWN_CATEGORY", "Rubrique inconnue."));
        String bodyHtml = HtmlSanitizer.clean(body.bodyHtml());
        String titleEn = blankToNull(body.titleEn());
        String bodyHtmlEn = titleEn == null || blankToNull(body.bodyHtmlEn()) == null ? null : HtmlSanitizer.clean(body.bodyHtmlEn());
        if (titleEn != null && bodyHtmlEn == null && !bodyHtml.isBlank()) {
            throw invalid("ENGLISH_INCOMPLETE", "La version anglaise doit avoir un titre ET un texte (ou rester vide).");
        }
        if (titleEn != null && bodyHtmlEn == null) {
            bodyHtmlEn = ""; // article sans texte (image seule) : la version anglaise n'a qu'un titre
        }
        String summaryEn = titleEn == null ? null : blankToNull(body.summaryEn());
        boolean enAuto = titleEn != null && current != null && current.enAuto() && Objects.equals(titleEn, current.titleEn())
                && Objects.equals(summaryEn, current.summaryEn()) && Objects.equals(bodyHtmlEn, current.bodyHtmlEn());
        Instant publishedAt = body.publishedAt() != null ? body.publishedAt()
                : current != null && current.publishedAt() != null ? current.publishedAt() : clock.instant();
        return new NewsChanges(body.title().strip(), blankToNull(body.summary()), bodyHtml, categoryId,
                checkedCover(body.coverKey(), current), body.published(), publishedAt, body.featured() && body.published(),
                titleEn, summaryEn, bodyHtmlEn, enAuto);
    }

    /** Image téléversée (clé connue de la médiathèque), ou image d'origine conservée telle quelle. */
    private @Nullable String checkedCover(@Nullable String key, @Nullable EditableNews current) {
        String value = blankToNull(key);
        if (value == null || (current != null && value.equals(current.coverKey()))) {
            return value;
        }
        boolean image = MediaKeys.isUuid(value) && media.find(UUID.fromString(value)).filter(MediaStorage.Media::isImage).isPresent();
        if (!image) {
            throw invalid("UNKNOWN_MEDIA", "Image introuvable : téléversez-la à nouveau.");
        }
        return value;
    }

    private void announce(String slug) {
        events.publishEvent(new ContentChanged(List.of("news", "news:" + slug)));
    }

    // -------------------------------------------------------------------------------------- rubriques

    @Transactional
    CategoryRow createCategory(CategoryRequest body) {
        String slug = Slugs.unique(Slugs.from(body.label()), repository::categorySlugTaken);
        UUID id = repository.insertCategory(slug, body.label().strip(), blankToNull(body.labelEn()));
        events.publishEvent(new ContentChanged(List.of("news")));
        return repository.category(id).orElseThrow();
    }

    @Transactional
    CategoryRow updateCategory(UUID id, CategoryRequest body) {
        category(id);
        repository.updateCategory(id, body.label().strip(), blankToNull(body.labelEn()));
        events.publishEvent(new ContentChanged(List.of("news")));
        return repository.category(id).orElseThrow();
    }

    @Transactional
    List<CategoryRow> reorderCategories(List<UUID> ids) {
        var known = repository.categories().stream().map(CategoryRow::id).toList();
        if (ids.size() != known.size() || !new HashSet<>(ids).equals(new HashSet<>(known))) {
            throw invalid("INCOMPLETE_ORDER", "L'ordre doit contenir chaque rubrique une seule fois.");
        }
        repository.reorderCategories(new ArrayList<>(ids));
        events.publishEvent(new ContentChanged(List.of("news")));
        return repository.categories();
    }

    @Transactional
    void deleteCategory(UUID id) {
        CategoryRow category = category(id);
        if (AdminNewsDtos.MENU_CATEGORIES.contains(category.slug())) {
            throw invalid("MENU_CATEGORY", "Cette rubrique a sa propre page dans le menu : elle ne peut pas être supprimée.");
        }
        if (category.articles() > 0) {
            throw invalid("CATEGORY_NOT_EMPTY", "Rangez d'abord les articles de cette rubrique dans une autre rubrique.");
        }
        repository.deleteCategory(id);
        events.publishEvent(new ContentChanged(List.of("news")));
    }

    private CategoryRow category(UUID id) {
        return repository.category(id).orElseThrow(() -> new ResourceNotFoundException("Rubrique", id.toString()));
    }
}
