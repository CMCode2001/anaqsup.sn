package sn.anaqsup.portail.comptes.security;

import java.io.Serial;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import sn.anaqsup.portail.comptes.Permission;
import sn.anaqsup.portail.comptes.persistence.AccountRepository.AccountRow;

/** Personne connectée au back-office. Ses droits sont relus en base à chaque requête (AdminSessionRefreshFilter). */
public final class AdminPrincipal implements UserDetails, CredentialsContainer {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String displayName;
    private final UUID roleId;
    private final String roleName;
    private final Set<Permission> permissions;
    private final boolean mustChangePassword;
    private final boolean enabled;
    private @Nullable String passwordHash;

    AdminPrincipal(AccountRow row) {
        this.id = row.id();
        this.email = row.email();
        this.displayName = row.displayName();
        this.roleId = row.roleId();
        this.roleName = row.roleName();
        this.permissions = Set.copyOf(row.permissions());
        this.mustChangePassword = row.mustChangePassword();
        this.enabled = row.enabled();
        this.passwordHash = row.passwordHash();
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    public UUID roleId() {
        return roleId;
    }

    public String roleName() {
        return roleName;
    }

    public Set<Permission> permissions() {
        return permissions;
    }

    /** Mot de passe provisoire : tant qu'il n'est pas changé, seul l'écran de changement est accessible. */
    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions.stream().map(p -> new SimpleGrantedAuthority(p.authority())).toList();
    }

    @Override
    public @Nullable String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        passwordHash = null;
    }

    /** Mêmes nom, rôle et droits que {@code other} (le mot de passe, effacé, n'entre pas en compte). */
    boolean sameAs(AdminPrincipal other) {
        return id.equals(other.id) && email.equals(other.email) && displayName.equals(other.displayName)
                && roleId.equals(other.roleId) && roleName.equals(other.roleName) && permissions.equals(other.permissions)
                && mustChangePassword == other.mustChangePassword && enabled == other.enabled;
    }
}
