package sn.anaqsup.portail.comptes.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import sn.anaqsup.portail.comptes.persistence.AccountRepository;

/**
 * Relit le compte en base à chaque requête du back-office : un compte désactivé ou supprimé est
 * déconnecté immédiatement, et un changement de rôle s'applique sans attendre une nouvelle connexion.
 * Une requête par appel, sur la clé primaire : coût négligeable pour quelques agents. La session, enregistrée
 * en base, n'est réécrite que si le compte a changé.
 */
final class AdminSessionRefreshFilter extends OncePerRequestFilter {

    private final AccountRepository accounts;
    private final SecurityContextRepository contextRepository;
    private final SecurityContextHolderStrategy holder = SecurityContextHolder.getContextHolderStrategy();

    AdminSessionRefreshFilter(AccountRepository accounts, SecurityContextRepository contextRepository) {
        this.accounts = accounts;
        this.contextRepository = contextRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = holder.getContext().getAuthentication();
        if (current != null && current.getPrincipal() instanceof AdminPrincipal principal) {
            var fresh = accounts.findById(principal.id()).filter(row -> row.enabled());
            if (fresh.isEmpty()) {
                holder.clearContext();
                var session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } else {
                var refreshed = new AdminPrincipal(fresh.get());
                refreshed.eraseCredentials();
                var authentication = UsernamePasswordAuthenticationToken.authenticated(refreshed, null, refreshed.getAuthorities());
                var context = holder.createEmptyContext();
                context.setAuthentication(authentication);
                holder.setContext(context);
                if (!refreshed.sameAs(principal)) {
                    contextRepository.saveContext(context, request, response);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
