package sn.anaqsup.portail.shared.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Toutes les erreurs sortent au format RFC 9457. Les erreurs Spring MVC standard
 * (validation, type de paramètre, méthode non supportée…) sont gérées par la classe mère ;
 * on ajoute ici le filet de sécurité pour les erreurs inattendues, sans fuite de détail interne.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception) {
        log.error("Erreur inattendue", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue.");
        problem.setType(Problems.type("internal"));
        problem.setTitle("Erreur interne");
        problem.setProperty("code", "INTERNAL_ERROR");
        return problem;
    }
}
