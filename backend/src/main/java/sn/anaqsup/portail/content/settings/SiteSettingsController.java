package sn.anaqsup.portail.content.settings;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Paramètres du site : lecture publique (bandeau, pied de page, page Contact, textes d'introduction) et
 * modification dans le back-office (permission PARAMETRES).
 */
@RestController
class SiteSettingsController {

    /** Paramètres et liste des textes d'introduction modifiables (pour l'écran du back-office). */
    record AdminSettings(SiteSettings settings, List<String> introKeys) {
    }

    private final SiteSettingsService service;

    SiteSettingsController(SiteSettingsService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/public/settings")
    ResponseEntity<SiteSettings> publicSettings() {
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic()).body(service.load());
    }

    @GetMapping("/api/v1/admin/settings")
    AdminSettings adminSettings() {
        return new AdminSettings(service.load(), SiteSettings.INTRO_KEYS);
    }

    @PutMapping("/api/v1/admin/settings")
    AdminSettings save(@Valid @RequestBody SiteSettings body, Authentication authentication) {
        return new AdminSettings(service.save(body, authentication.getName()), SiteSettings.INTRO_KEYS);
    }
}
