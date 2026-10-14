package sn.anaqsup.portail.content.admin;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.content.admin.AdminContentDtos.Page;
import sn.anaqsup.portail.content.admin.AdminContentDtos.PageRequest;
import sn.anaqsup.portail.content.admin.AdminContentDtos.PageSummary;
import sn.anaqsup.portail.content.admin.AdminContentDtos.Partner;
import sn.anaqsup.portail.content.admin.AdminContentDtos.PartnerOrder;
import sn.anaqsup.portail.content.admin.AdminContentDtos.PartnerRequest;
import sn.anaqsup.portail.content.persistence.AdminContentRepository;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Back-office : pages institutionnelles (permission PAGES) et partenaires (permission PARTENAIRES).
 * Les permissions sont vérifiées par la chaîne de sécurité de l'administration (module comptes).
 */
@RestController
@RequestMapping("/api/v1/admin")
class AdminContentController {

    private final AdminContentRepository repository;
    private final AdminContentService service;

    AdminContentController(AdminContentRepository repository, AdminContentService service) {
        this.repository = repository;
        this.service = service;
    }

    @GetMapping("/pages")
    List<PageSummary> pages() {
        return repository.pages().stream().map(PageSummary::from).toList();
    }

    @GetMapping("/pages/{slug}")
    Page page(@PathVariable String slug) {
        return repository.page(slug).map(Page::from).orElseThrow(() -> new ResourceNotFoundException("Page", slug));
    }

    @PutMapping("/pages/{slug}")
    Page updatePage(@PathVariable String slug, @Valid @RequestBody PageRequest body, Authentication authentication) {
        return Page.from(service.updatePage(slug, body, authentication.getName()));
    }

    @GetMapping("/partners")
    List<Partner> partners() {
        return repository.partners().stream().map(Partner::from).toList();
    }

    @PostMapping("/partners")
    ResponseEntity<Partner> createPartner(@Valid @RequestBody PartnerRequest body) {
        var created = Partner.from(service.createPartner(body));
        return ResponseEntity.created(URI.create("/api/v1/admin/partners/" + created.id())).body(created);
    }

    @PutMapping("/partners/order")
    List<Partner> reorder(@Valid @RequestBody PartnerOrder body) {
        return service.reorderPartners(body.ids()).stream().map(Partner::from).toList();
    }

    @PutMapping("/partners/{id}")
    Partner updatePartner(@PathVariable UUID id, @Valid @RequestBody PartnerRequest body) {
        return Partner.from(service.updatePartner(id, body));
    }

    @DeleteMapping("/partners/{id}")
    ResponseEntity<Void> deletePartner(@PathVariable UUID id) {
        service.deletePartner(id);
        return ResponseEntity.noContent().build();
    }
}
