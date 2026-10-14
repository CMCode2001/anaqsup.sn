package sn.anaqsup.portail.comptes;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.anaqsup.portail.comptes.persistence.AccountRepository;
import sn.anaqsup.portail.comptes.persistence.AccountRepository.AccountRow;
import sn.anaqsup.portail.comptes.persistence.AccountRepository.RoleRow;
import sn.anaqsup.portail.shared.web.ResourceNotFoundException;

/**
 * Règles de gestion des comptes et des rôles (décisions ANAQ-Sup du 13/10/2026) :
 * <ul>
 *   <li>comptes créés par l'administrateur avec un mot de passe provisoire, à changer à la première connexion ;</li>
 *   <li>mot de passe de {@value #MIN_PASSWORD_LENGTH} caractères minimum ;</li>
 *   <li>le rôle Administrateur ne se modifie pas ; il reste toujours au moins un administrateur actif ;</li>
 *   <li>on ne peut ni se supprimer, ni se désactiver, ni changer son propre rôle (pas de perte d'accès par erreur).</li>
 * </ul>
 */
@Service
public class AccountService {

    public static final int MIN_PASSWORD_LENGTH = 8;

    private final AccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    AccountService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------------------------------------- comptes

    public List<AccountRow> accounts() {
        return accounts.accounts();
    }

    public AccountRow account(UUID id) {
        return accounts.findById(id).orElseThrow(() -> new ResourceNotFoundException("Compte", id.toString()));
    }

    @Transactional
    public AccountRow createAccount(String email, String displayName, UUID roleId, String temporaryPassword) {
        requireRole(roleId);
        checkPassword(temporaryPassword);
        if (accounts.emailExists(email)) {
            throw new AccountRuleException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Un compte existe déjà avec cette adresse e-mail.");
        }
        UUID id = accounts.insertAccount(email, displayName, passwordEncoder.encode(temporaryPassword), roleId);
        return account(id);
    }

    @Transactional
    public AccountRow updateAccount(UUID actorId, UUID id, String displayName, UUID roleId, boolean enabled) {
        AccountRow current = account(id);
        RoleRow role = requireRole(roleId);
        if (actorId.equals(id) && (!enabled || !roleId.equals(current.roleId()))) {
            throw new AccountRuleException(HttpStatus.CONFLICT, "SELF_PROTECTED",
                    "Vous ne pouvez ni désactiver votre propre compte, ni changer votre propre rôle.");
        }
        boolean losesAdministrator = current.roleSystem() && current.enabled() && (!role.system() || !enabled);
        if (losesAdministrator && accounts.countEnabledAdministrators() <= 1) {
            throw lastAdministrator();
        }
        accounts.updateAccount(id, displayName, roleId, enabled);
        return account(id);
    }

    /** Nouveau mot de passe provisoire, à changer à la prochaine connexion. */
    @Transactional
    public void resetPassword(UUID id, String temporaryPassword) {
        account(id);
        checkPassword(temporaryPassword);
        accounts.updatePassword(id, passwordEncoder.encode(temporaryPassword), true);
    }

    @Transactional
    public void deleteAccount(UUID actorId, UUID id) {
        AccountRow current = account(id);
        if (actorId.equals(id)) {
            throw new AccountRuleException(HttpStatus.CONFLICT, "SELF_PROTECTED", "Vous ne pouvez pas supprimer votre propre compte.");
        }
        if (current.roleSystem() && current.enabled() && accounts.countEnabledAdministrators() <= 1) {
            throw lastAdministrator();
        }
        accounts.deleteAccount(id);
    }

    /** Changement de son propre mot de passe (obligatoire après un mot de passe provisoire). */
    @Transactional
    public void changeOwnPassword(UUID id, String currentPassword, String newPassword) {
        AccountRow account = account(id);
        if (!passwordEncoder.matches(currentPassword, account.passwordHash())) {
            throw new AccountRuleException(HttpStatus.BAD_REQUEST, "WRONG_PASSWORD", "Le mot de passe actuel est incorrect.");
        }
        checkPassword(newPassword);
        if (passwordEncoder.matches(newPassword, account.passwordHash())) {
            throw new AccountRuleException(HttpStatus.BAD_REQUEST, "SAME_PASSWORD",
                    "Le nouveau mot de passe doit être différent de l'actuel.");
        }
        accounts.updatePassword(id, passwordEncoder.encode(newPassword), false);
    }

    // ------------------------------------------------------------------------------------------ rôles

    public List<RoleRow> roles() {
        return accounts.roles();
    }

    @Transactional
    public RoleRow createRole(String name, Set<Permission> permissions) {
        checkRole(name, permissions, null);
        return accounts.findRole(accounts.insertRole(name, permissions)).orElseThrow();
    }

    @Transactional
    public RoleRow updateRole(UUID id, String name, Set<Permission> permissions) {
        RoleRow role = requireRole(id);
        if (role.system()) {
            throw systemRole();
        }
        checkRole(name, permissions, id);
        accounts.updateRole(id, name, permissions);
        return requireRole(id);
    }

    @Transactional
    public void deleteRole(UUID id) {
        RoleRow role = requireRole(id);
        if (role.system()) {
            throw systemRole();
        }
        if (role.accountCount() > 0) {
            throw new AccountRuleException(HttpStatus.CONFLICT, "ROLE_IN_USE",
                    "Ce rôle est attribué à %d compte(s) : changez-leur de rôle avant de le supprimer.".formatted(role.accountCount()));
        }
        accounts.deleteRole(id);
    }

    // ------------------------------------------------------------------------------------------ règles

    private RoleRow requireRole(UUID id) {
        return accounts.findRole(id).orElseThrow(() -> new ResourceNotFoundException("Rôle", id.toString()));
    }

    private void checkRole(String name, Set<Permission> permissions, @Nullable UUID exceptId) {
        if (permissions.isEmpty()) {
            throw new AccountRuleException(HttpStatus.BAD_REQUEST, "NO_PERMISSION", "Choisissez au moins une permission.");
        }
        if (accounts.roleNameExists(name, exceptId)) {
            throw new AccountRuleException(HttpStatus.CONFLICT, "ROLE_NAME_TAKEN", "Un rôle porte déjà ce nom.");
        }
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new AccountRuleException(HttpStatus.BAD_REQUEST, "PASSWORD_TOO_SHORT",
                    "Le mot de passe doit contenir au moins %d caractères.".formatted(MIN_PASSWORD_LENGTH));
        }
    }

    private static AccountRuleException lastAdministrator() {
        return new AccountRuleException(HttpStatus.CONFLICT, "LAST_ADMINISTRATOR",
                "Il doit rester au moins un administrateur actif.");
    }

    private static AccountRuleException systemRole() {
        return new AccountRuleException(HttpStatus.CONFLICT, "SYSTEM_ROLE",
                "Le rôle Administrateur a tous les droits et ne peut être ni modifié, ni supprimé.");
    }
}
