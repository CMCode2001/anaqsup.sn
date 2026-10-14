package sn.anaqsup.portail.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Rendue en {@code application/problem+json} (RFC 9457) avec le code {@code RESOURCE_NOT_FOUND}. */
public class ResourceNotFoundException extends ErrorResponseException {

    public ResourceNotFoundException(String resource, String identifier) {
        super(HttpStatus.NOT_FOUND, problem(resource, identifier), null);
    }

    private static ProblemDetail problem(String resource, String identifier) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "%s « %s » introuvable.".formatted(resource, identifier));
        problem.setType(Problems.type("not-found"));
        problem.setTitle("Ressource introuvable");
        problem.setProperty("code", "RESOURCE_NOT_FOUND");
        return problem;
    }
}
