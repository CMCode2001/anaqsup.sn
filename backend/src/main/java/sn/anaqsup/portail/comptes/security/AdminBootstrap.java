package sn.anaqsup.portail.comptes.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import sn.anaqsup.portail.comptes.AccountService;
import sn.anaqsup.portail.comptes.persistence.AccountRepository;

/** Crée le premier administrateur au démarrage, si la base n'a encore aucun compte. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;
    private final AdminBootstrapProperties properties;

    AdminBootstrap(AccountRepository accounts, PasswordEncoder passwordEncoder, AdminBootstrapProperties properties) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accounts.countAccounts() > 0) {
            return;
        }
        if (!properties.configured()) {
            log.warn("Aucun compte du back-office : définir ADMIN_BOOTSTRAP_EMAIL et ADMIN_BOOTSTRAP_PASSWORD pour créer le premier administrateur.");
            return;
        }
        if (properties.password().length() < AccountService.MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_PASSWORD : %d caractères minimum.".formatted(AccountService.MIN_PASSWORD_LENGTH));
        }
        String name = properties.name() == null || properties.name().isBlank() ? "Administrateur" : properties.name();
        accounts.insertAccount(properties.email(), name, passwordEncoder.encode(properties.password()), accounts.administratorRoleId());
        // Jamais le mot de passe dans les journaux.
        log.info("Premier administrateur créé pour {} (mot de passe provisoire, à changer à la première connexion).", properties.email());
    }
}
