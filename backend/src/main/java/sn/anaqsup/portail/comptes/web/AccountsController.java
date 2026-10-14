package sn.anaqsup.portail.comptes.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.comptes.AccountService;
import sn.anaqsup.portail.comptes.security.AdminPrincipal;
import sn.anaqsup.portail.comptes.web.AdminDtos.Account;
import sn.anaqsup.portail.comptes.web.AdminDtos.CreateAccountRequest;
import sn.anaqsup.portail.comptes.web.AdminDtos.ResetPasswordRequest;
import sn.anaqsup.portail.comptes.web.AdminDtos.Role;
import sn.anaqsup.portail.comptes.web.AdminDtos.RoleRequest;
import sn.anaqsup.portail.comptes.web.AdminDtos.UpdateAccountRequest;

/** Gestion des comptes et des rôles : permission COMPTES (rôle Administrateur par défaut). */
@RestController
@RequestMapping("/api/v1/admin")
class AccountsController {

    private final AccountService service;

    AccountsController(AccountService service) {
        this.service = service;
    }

    // ---------------------------------------------------------------------------------------- comptes

    @GetMapping("/accounts")
    List<Account> accounts() {
        return service.accounts().stream().map(Account::from).toList();
    }

    @PostMapping("/accounts")
    ResponseEntity<Account> create(@Valid @RequestBody CreateAccountRequest body) {
        var created = Account.from(service.createAccount(body.email(), body.displayName(), body.roleId(), body.temporaryPassword()));
        return ResponseEntity.created(URI.create("/api/v1/admin/accounts/" + created.id())).body(created);
    }

    @PutMapping("/accounts/{id}")
    Account update(@AuthenticationPrincipal AdminPrincipal actor, @PathVariable UUID id, @Valid @RequestBody UpdateAccountRequest body) {
        return Account.from(service.updateAccount(actor.id(), id, body.displayName(), body.roleId(), body.enabled()));
    }

    @PostMapping("/accounts/{id}/password")
    ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest body) {
        service.resetPassword(id, body.temporaryPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/accounts/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AdminPrincipal actor, @PathVariable UUID id) {
        service.deleteAccount(actor.id(), id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------------------------------ rôles

    @GetMapping("/roles")
    List<Role> roles() {
        return service.roles().stream().map(Role::from).toList();
    }

    @PostMapping("/roles")
    ResponseEntity<Role> createRole(@Valid @RequestBody RoleRequest body) {
        var created = Role.from(service.createRole(body.name(), body.permissions()));
        return ResponseEntity.created(URI.create("/api/v1/admin/roles/" + created.id())).body(created);
    }

    @PutMapping("/roles/{id}")
    Role updateRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest body) {
        return Role.from(service.updateRole(id, body.name(), body.permissions()));
    }

    @DeleteMapping("/roles/{id}")
    ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        service.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
