package sn.anaqsup.portail.comptes.web;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import sn.anaqsup.portail.comptes.Permission;
import sn.anaqsup.portail.comptes.persistence.AccountRepository.AccountRow;
import sn.anaqsup.portail.comptes.persistence.AccountRepository.RoleRow;
import sn.anaqsup.portail.comptes.security.AdminPrincipal;

/** Corps des requêtes et réponses de l'API d'administration (contrat : contracts/openapi/admin-api.v1.yaml). */
final class AdminDtos {

    private AdminDtos() {
    }

    // ----------------------------------------------------------------------------------- requêtes

    record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {
    }

    record ChangePasswordRequest(@NotBlank @Size(max = 128) String currentPassword, @NotBlank @Size(max = 128) String newPassword) {
    }

    record CreateAccountRequest(@NotBlank @Email @Size(max = 254) String email,
                                @NotBlank @Size(max = 120) String displayName,
                                @NotNull UUID roleId,
                                @NotBlank @Size(max = 128) String temporaryPassword) {
    }

    record UpdateAccountRequest(@NotBlank @Size(max = 120) String displayName, @NotNull UUID roleId, boolean enabled) {
    }

    record ResetPasswordRequest(@NotBlank @Size(max = 128) String temporaryPassword) {
    }

    record RoleRequest(@NotBlank @Size(min = 2, max = 60) String name, @NotNull Set<Permission> permissions) {
    }

    // ----------------------------------------------------------------------------------- réponses

    record RoleRef(UUID id, String name) {
    }

    record Me(UUID id, String email, String displayName, RoleRef role, Set<Permission> permissions, boolean mustChangePassword) {
        static Me from(AdminPrincipal principal) {
            return new Me(principal.id(), principal.email(), principal.displayName(),
                    new RoleRef(principal.roleId(), principal.roleName()), principal.permissions(), principal.mustChangePassword());
        }

        static Me from(AccountRow row) {
            return new Me(row.id(), row.email(), row.displayName(), new RoleRef(row.roleId(), row.roleName()),
                    row.permissions(), row.mustChangePassword());
        }
    }

    record Account(UUID id, String email, String displayName, RoleRef role, boolean enabled, boolean mustChangePassword,
                   @Nullable Instant lastLoginAt, Instant createdAt) {
        static Account from(AccountRow row) {
            return new Account(row.id(), row.email(), row.displayName(), new RoleRef(row.roleId(), row.roleName()),
                    row.enabled(), row.mustChangePassword(), row.lastLoginAt(), row.createdAt());
        }
    }

    record Role(UUID id, String name, Set<Permission> permissions, boolean system, int accountCount) {
        static Role from(RoleRow row) {
            return new Role(row.id(), row.name(), row.permissions(), row.system(), row.accountCount());
        }
    }
}
