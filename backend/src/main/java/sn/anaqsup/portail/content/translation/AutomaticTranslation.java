package sn.anaqsup.portail.content.translation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import sn.anaqsup.portail.content.ContentChanged;
import sn.anaqsup.portail.content.MachineTranslator;
import sn.anaqsup.portail.content.admin.HtmlSanitizer;
import sn.anaqsup.portail.content.persistence.TranslationRepository;
import sn.anaqsup.portail.content.persistence.TranslationRepository.English;
import sn.anaqsup.portail.content.persistence.TranslationRepository.Pending;

/**
 * Traduction automatique vers l'anglais (décision ANAQ-Sup du 14/10/2026) : après chaque enregistrement
 * dans le back-office, et toutes les 15 minutes en rattrapage (service indisponible, quota atteint…),
 * chaque contenu sans version anglaise est traduit par DeepL. Le portail est prévenu contenu par contenu.
 */
@Component
class AutomaticTranslation {

    private static final Logger log = LoggerFactory.getLogger(AutomaticTranslation.class);

    /** Contenus traduits au plus par type et par passage : un import massif ne vide pas le quota d'un coup. */
    private static final int BATCH = 25;

    private final ObjectProvider<MachineTranslator> translators;
    private final TranslationRepository repository;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    /** Un seul passage à la fois (événement et rattrapage peuvent se croiser). */
    private final ReentrantLock running = new ReentrantLock();
    /** Âge maximal (en mois) d'une actualité traduite automatiquement. */
    private final int newsMaxAgeMonths;

    AutomaticTranslation(ObjectProvider<MachineTranslator> translators, TranslationRepository repository,
                         ApplicationEventPublisher events, PlatformTransactionManager transactions,
                         @Value("${anaqsup.translation.news-max-age-months:12}") int newsMaxAgeMonths) {
        this.newsMaxAgeMonths = newsMaxAgeMonths;
        this.translators = translators;
        this.repository = repository;
        this.events = events;
        this.transaction = new TransactionTemplate(transactions);
        // Chaque traduction est validée aussitôt, indépendamment du reste du passage.
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @ApplicationModuleListener
    void on(ContentChanged event) {
        translatePending();
    }

    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    void catchUp() {
        translatePending();
    }

    /** @return nombre de contenus traduits pendant ce passage */
    int translatePending() {
        MachineTranslator translator = translators.getIfAvailable();
        if (translator == null || !translator.available() || !running.tryLock()) {
            return 0;
        }
        int translated = 0;
        try {
            for (TranslationRepository.Type type : TranslationRepository.Type.values()) {
                for (Pending item : repository.pending(type, BATCH, newsMaxAgeMonths)) {
                    English english = translate(translator, item);
                    Boolean saved = transaction.execute(status -> {
                        boolean done = repository.save(item, english);
                        if (done) {
                            events.publishEvent(new ContentChanged(type.tags(item.slug())));
                        }
                        return done;
                    });
                    if (Boolean.TRUE.equals(saved)) {
                        translated++;
                    }
                }
            }
        } catch (MachineTranslator.UnavailableException e) {
            log.warn("Traduction automatique interrompue, reprise au prochain passage : {}", e.getMessage());
        } finally {
            running.unlock();
        }
        if (translated > 0) {
            log.info("Traduction automatique : {} contenu(s) traduit(s) en anglais", translated);
        }
        return translated;
    }

    private static English translate(MachineTranslator translator, Pending item) {
        List<String> texts = new ArrayList<>();
        texts.add(item.title());
        if (item.summary() != null) {
            texts.add(item.summary());
        }
        if (item.role() != null) {
            texts.add(item.role());
        }
        Iterator<String> english = translator.toEnglish(texts, false).iterator();
        String title = english.next();
        String summary = item.summary() == null ? null : english.next();
        String role = item.role() == null ? null : english.next();
        return new English(title, summary, body(translator, item.bodyHtml()), role);
    }

    /** Le HTML traduit repasse par la liste blanche, comme tout HTML saisi dans le back-office. */
    private static @Nullable String body(MachineTranslator translator, @Nullable String html) {
        if (html == null) {
            return null;
        }
        return html.isBlank() ? html : HtmlSanitizer.clean(translator.toEnglish(List.of(html), true).getFirst());
    }
}
