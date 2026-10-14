package sn.anaqsup.portail.content.admin;

import java.net.URI;
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

import sn.anaqsup.portail.content.admin.AdminPublicationDtos.Publication;
import sn.anaqsup.portail.content.admin.AdminPublicationDtos.PublicationRequest;
import sn.anaqsup.portail.content.persistence.AdminPublicationRepository;
import sn.anaqsup.portail.shared.web.PageResponse;

/** Back-office : ressources documentaires (permission DOCUMENTS, vérifiée par la chaîne de sécurité). */
@RestController
@RequestMapping("/api/v1/admin/publications")
class AdminPublicationsController {

    private final AdminPublicationRepository repository;
    private final AdminPublicationService service;

    AdminPublicationsController(AdminPublicationRepository repository, AdminPublicationService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping
    PageResponse<Publication> list(@RequestParam(required = false) @Size(max = 100) @Nullable String q,
                                   @RequestParam(required = false) @Pattern(regexp = AdminPublicationDtos.KINDS) @Nullable String kind,
                                   @RequestParam(defaultValue = "0") @Min(0) int page,
                                   @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var slice = repository.list(q, kind, page, size);
        return PageResponse.of(slice.rows().stream().map(Publication::from).toList(), page, size, slice.total());
    }

    @GetMapping("/{id}")
    Publication get(@PathVariable UUID id) {
        return Publication.from(service.find(id));
    }

    @PostMapping
    ResponseEntity<Publication> create(@Valid @RequestBody PublicationRequest body, Authentication authentication) {
        var created = Publication.from(service.create(body, authentication.getName()));
        return ResponseEntity.created(URI.create("/api/v1/admin/publications/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    Publication update(@PathVariable UUID id, @Valid @RequestBody PublicationRequest body, Authentication authentication) {
        return Publication.from(service.update(id, body, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
