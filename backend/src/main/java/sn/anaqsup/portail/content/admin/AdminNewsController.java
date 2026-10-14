package sn.anaqsup.portail.content.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.content.admin.AdminNewsDtos.Category;
import sn.anaqsup.portail.content.admin.AdminNewsDtos.CategoryOrder;
import sn.anaqsup.portail.content.admin.AdminNewsDtos.CategoryRequest;
import sn.anaqsup.portail.content.admin.AdminNewsDtos.News;
import sn.anaqsup.portail.content.admin.AdminNewsDtos.NewsRequest;
import sn.anaqsup.portail.content.persistence.AdminNewsRepository;
import sn.anaqsup.portail.shared.web.PageResponse;

/** Back-office : actualités et rubriques (permission ACTUALITES, vérifiée par la chaîne de sécurité). */
@RestController
@RequestMapping("/api/v1/admin/news")
class AdminNewsController {

    private final AdminNewsRepository repository;
    private final AdminNewsService service;

    AdminNewsController(AdminNewsRepository repository, AdminNewsService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    PageResponse<News> list(@RequestParam(required = false) @Size(max = 100) @Nullable String q,
                            @RequestParam(required = false) @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$") @Nullable String category,
                            @RequestParam(defaultValue = "0") @Min(0) int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var slice = repository.list(q, category, page, size);
        return PageResponse.of(slice.rows().stream().map(News::from).toList(), page, size, slice.total());
    }

    @GetMapping("/{id}")
    News get(@PathVariable UUID id) {
        return News.from(service.find(id));
    }

    @PostMapping
    ResponseEntity<News> create(@Valid @RequestBody NewsRequest body, Authentication authentication) {
        var created = News.from(service.create(body, authentication.getName()));
        return ResponseEntity.created(URI.create("/api/v1/admin/news/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    News update(@PathVariable UUID id, @Valid @RequestBody NewsRequest body, Authentication authentication) {
        return News.from(service.update(id, body, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------------------- rubriques

    @GetMapping("/categories")
    List<Category> categories() {
        return repository.categories().stream().map(Category::from).toList();
    }

    @PostMapping("/categories")
    ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest body) {
        var created = Category.from(service.createCategory(body));
        return ResponseEntity.created(URI.create("/api/v1/admin/news/categories/" + created.id())).body(created);
    }

    @PutMapping("/categories/order")
    List<Category> reorder(@Valid @RequestBody CategoryOrder body) {
        return service.reorderCategories(body.ids()).stream().map(Category::from).toList();
    }

    @PutMapping("/categories/{id}")
    Category updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest body) {
        return Category.from(service.updateCategory(id, body));
    }

    @DeleteMapping("/categories/{id}")
    ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
