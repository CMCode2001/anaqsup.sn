package sn.anaqsup.portail.comptes;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import sn.anaqsup.portail.shared.web.Problems;

/** Règle de gestion des comptes non respectée : réponse RFC 9457 avec un code stable pour le back-office. */
public class AccountRuleException extends ErrorResponseException {

    public AccountRuleException(HttpStatus status, String code, String detail) {
        super(status, problem(status, code, detail), null);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(Problems.type("admin/" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(status == HttpStatus.CONFLICT ? "Opération impossible" : "Requête refusée");
        problem.setProperty("code", code);
        return problem;
    }
}
