package sn.anaqsup.portail.content.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.content.settings.SiteSettings.Contact;
import sn.anaqsup.portail.content.settings.SiteSettings.Intro;
import sn.anaqsup.portail.content.settings.SiteSettings.SocialLink;
import sn.anaqsup.portail.content.settings.SiteSettings.UsefulLink;
import sn.anaqsup.portail.shared.web.Problems;

/** Lecture et enregistrement des paramètres du site (table {@code site_setting}, une ligne JSON par rubrique). */
@Service
public class SiteSettingsService {

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final ApplicationEventPublisher events;

    SiteSettingsService(JdbcClient jdbc, JsonMapper json, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.json = json;
        this.events = events;
    }

    public SiteSettings load() {
        Map<String, String> rows = new LinkedHashMap<>();
        jdbc.sql("SELECT key, value::text AS value FROM site_setting")
                .query((rs, n) -> rows.put(rs.getString("key"), rs.getString("value")))
                .list();
        Contact contact = json.readValue(rows.getOrDefault("contact", "{}"), Contact.class);
        List<SocialLink> social = json.readValue(rows.getOrDefault("social", "[]"), new TypeReference<>() { });
        List<UsefulLink> links = json.readValue(rows.getOrDefault("useful_links", "[]"), new TypeReference<>() { });
        Map<String, Intro> intros = json.readValue(rows.getOrDefault("intros", "{}"), new TypeReference<>() { });
        return new SiteSettings(contact, social, links, intros);
    }

    /** Enregistre tout, d'un bloc ; le portail est prévenu (bandeau, pied de page, pages). */
    @Transactional
    public SiteSettings save(SiteSettings settings, String by) {
        Map<String, Intro> intros = new LinkedHashMap<>();
        settings.intros().forEach((key, intro) -> {
            if (!SiteSettings.INTRO_KEYS.contains(key)) {
                throw invalid("UNKNOWN_INTRO", "Texte d'introduction inconnu : " + key);
            }
            Intro cleaned = new Intro(blankToNull(intro.fr()), blankToNull(intro.en()));
            if (cleaned.fr() != null || cleaned.en() != null) {
                intros.put(key, cleaned);
            }
        });
        if (settings.social().stream().map(SocialLink::network).distinct().count() != settings.social().size()) {
            throw invalid("DUPLICATE_NETWORK", "Chaque réseau social ne peut figurer qu'une fois.");
        }
        Contact c = settings.contact();
        write("contact", new Contact(c.phone().strip(), c.email().strip(), c.address().strip(), blankToNull(c.addressEn())), by);
        write("social", settings.social(), by);
        write("useful_links", settings.usefulLinks().stream().map(l -> new UsefulLink(l.label().strip(), blankToNull(l.labelEn()),
                blankToNull(l.description()), blankToNull(l.descriptionEn()), l.url().strip())).toList(), by);
        write("intros", intros, by);
        events.publishEvent(new ContentChanged(List.of("settings")));
        return load();
    }

    private void write(String key, Object value, String by) {
        jdbc.sql("""
                        INSERT INTO site_setting (key, value, updated_at, updated_by)
                        VALUES (:key, CAST(:value AS jsonb), now(), :by)
                        ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, updated_at = now(), updated_by = EXCLUDED.updated_by""")
                .param("key", key).param("value", json.writeValueAsString(value)).param("by", by)
                .update();
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static ErrorResponseException invalid(String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(Problems.type("admin/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle("Requête refusée");
        problem.setProperty("code", code);
        return new ErrorResponseException(HttpStatus.BAD_REQUEST, problem, null);
    }
}
