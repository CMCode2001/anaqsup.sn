package sn.anaqsup.portail.content.settings;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

/**
 * Paramètres du site, modifiables dans le back-office (décision ANAQ-Sup du 14/10/2026) : coordonnées,
 * réseaux sociaux, liens utiles du pied de page et textes d'introduction des rubriques.
 */
public record SiteSettings(@NotNull @Valid Contact contact,
                           @NotNull @Size(max = 8) List<@Valid SocialLink> social,
                           @NotNull @Size(max = 12) List<@Valid UsefulLink> usefulLinks,
                           @NotNull Map<String, @Valid Intro> intros) {

    /**
     * Textes d'introduction modifiables : clé = texte d'origine du site (messages du portail). Seulement ceux
     * affichés sous un titre (les pages Actualités n'ont pas de texte d'introduction).
     */
    public static final List<String> INTRO_KEYS = List.of(
            "Home.approvalsLead", "Home.partnersLead", "Events.lead",
            "Resources.publicationsLead", "Resources.regulationsLead", "Resources.standardsLead", "Resources.selfAssessmentLead",
            "Approvals.lead", "Institutions.lead", "Programmes.lead");

    public record Contact(@NotBlank @Size(max = 40) String phone,
                          @NotBlank @Email @Size(max = 120) String email,
                          @NotBlank @Size(max = 300) String address,
                          @Nullable @Size(max = 300) String addressEn) {
    }

    public record SocialLink(@NotBlank @Pattern(regexp = "facebook|x|linkedin|youtube") String network,
                             @NotBlank @Size(max = 300) @Pattern(regexp = "^https://\\S+$") String url) {
    }

    public record UsefulLink(@NotBlank @Size(max = 80) String label,
                             @Nullable @Size(max = 80) String labelEn,
                             @Nullable @Size(max = 200) String description,
                             @Nullable @Size(max = 200) String descriptionEn,
                             @NotBlank @Size(max = 300) @Pattern(regexp = "^https?://\\S+$") String url) {
    }

    /** Texte d'introduction : vide = le texte d'origine du site s'affiche. */
    public record Intro(@Nullable @Size(max = 600) String fr, @Nullable @Size(max = 600) String en) {
    }
}
