package org.keycloak.services;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.services.error.ProblemDetail;
import org.keycloak.services.error.ProblemType;

/**
 * An error of the Admin API v2 that is reported to the client as a problem of the given {@link ProblemType}.
 * <p>
 * The message is used as the {@code detail} of the problem, unless the type is a server error: the text of an unexpected
 * error is never sent to the client.
 */
public class ServiceException extends RuntimeException {

    private final ProblemType type;
    private final List<ProblemDetail.Violation> violations;

    public ServiceException(ProblemType type, String message) {
        super(message);
        this.type = type;
        this.violations = null;
    }

    public ServiceException(ProblemType type) {
        super();
        this.type = type;
        this.violations = null;
    }

    private ServiceException(List<ProblemDetail.Violation> violations) {
        super(violations.stream().map(ProblemDetail.Violation::message).collect(Collectors.joining("; ")));
        this.type = ProblemType.VALIDATION_FAILED;
        this.violations = violations;
    }

    /**
     * A {@link ProblemType#VALIDATION_FAILED} problem that points at a field of the request body or at a query parameter.
     */
    public static ServiceException invalid(String field, String message) {
        return invalid(List.of(new ProblemDetail.Violation(field, message)));
    }

    public static ServiceException invalid(List<ProblemDetail.Violation> violations) {
        return new ServiceException(violations);
    }

    public ProblemType getType() {
        return type;
    }

    /**
     * @return the violations to report, or {@code null} when the problem does not point at specific fields
     */
    public List<ProblemDetail.Violation> getViolations() {
        return violations;
    }

}
