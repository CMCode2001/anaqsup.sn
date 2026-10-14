package sn.anaqsup.portail.comptes.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Premier compte administrateur ({@code anaqsup.admin.bootstrap.*}), créé seulement si aucun compte
 * n'existe. Le mot de passe est provisoire : il doit être changé à la première connexion.
 */
@ConfigurationProperties(prefix = "anaqsup.admin.bootstrap")
record AdminBootstrapProperties(String email, String password, String name) {

    boolean configured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
