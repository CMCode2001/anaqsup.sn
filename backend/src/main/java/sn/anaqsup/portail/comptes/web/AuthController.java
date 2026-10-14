package sn.anaqsup.portail.comptes.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.anaqsup.portail.comptes.AccountRuleException;
import sn.anaqsup.portail.comptes.AccountService;
import sn.anaqsup.portail.comptes.persistence.AccountRepository;
import sn.anaqsup.portail.comptes.security.AdminPrincipal;
import sn.anaqsup.portail.comptes.web.AdminDtos.ChangePasswordRequest;
import sn.anaqsup.portail.comptes.web.AdminDtos.LoginRequest;
import sn.anaqsup.portail.comptes.web.AdminDtos.Me;

/** Connexion, profil et changement de mot de passe de la personne connectée. */
@RestController
@RequestMapping("/api/v1/admin/auth")
class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contextRepository;
    private final AccountRepository accounts;
    private final AccountService accountService;
    private final CookieCsrfTokenRepository csrfTokens;
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();

    AuthController(AuthenticationManager authenticationManager, SessionAuthenticationStrategy sessionStrategy,
                   SecurityContextRepository contextRepository, AccountRepository accounts, AccountService accountService,
                   CookieCsrfTokenRepository csrfTokens) {
        this.authenticationManager = authenticationManager;
        this.sessionStrategy = sessionStrategy;
        this.contextRepository = contextRepository;
        this.accounts = accounts;
        this.accountService = accountService;
        this.csrfTokens = csrfTokens;
    }

    @PostMapping("/login")
    Me login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(body.email().trim(), body.password()));
        } catch (AuthenticationException e) {
            // Même message pour un e-mail inconnu, un mauvais mot de passe ou un compte désactivé.
            throw new AccountRuleException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "E-mail ou mot de passe incorrect.");
        }
        // Nouvel identifiant de session et nouveau jeton CSRF, puis enregistrement de la connexion en session.
        sessionStrategy.onAuthentication(authentication, request, response);
        // Nouveau jeton CSRF à chaque connexion, posé dans cette réponse (cookie XSRF-TOKEN) : l'ancien,
        // connu avant la connexion, n'est plus valable, et la requête suivante du back-office a déjà le bon.
        csrfTokens.saveToken(csrfTokens.generateToken(request), request, response);
        var context = holder.createEmptyContext();
        context.setAuthentication(authentication);
        holder.setContext(context);
        contextRepository.saveContext(context, request, response);

        var principal = (AdminPrincipal) authentication.getPrincipal();
        accounts.touchLastLogin(principal.id());
        return Me.from(principal);
    }

    @GetMapping("/me")
    Me me(@AuthenticationPrincipal AdminPrincipal principal) {
        return Me.from(principal);
    }

    @PostMapping("/password")
    ResponseEntity<Void> changePassword(@AuthenticationPrincipal AdminPrincipal principal,
                                        @Valid @RequestBody ChangePasswordRequest body) {
        accountService.changeOwnPassword(principal.id(), body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
