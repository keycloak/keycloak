package org.keycloak.services.error;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.services.ServiceException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.jboss.logging.Logger;

/**
 * Formats every error of the Admin API v2 as an RFC 9457 problem, see {@link ProblemDetail} and {@link ProblemType}.
 * <p>
 * The text of exceptions that this API does not throw itself (JAX-RS, Jackson, the persistence layer) is not copied
 * into the response, because it can expose framework internals or echo input. Only a {@link ServiceException} and the
 * constraint violations carry text that was written for the client.
 */
public class ProblemDetailErrorFormatter implements AdminApiV2ErrorFormatter {

    private static final Logger logger = Logger.getLogger(ProblemDetailErrorFormatter.class);

    static final String INTERNAL_ERROR_DETAIL = "Internal server error, consult the server log";
    static final String MALFORMED_BODY_DETAIL = "Cannot parse the JSON";
    static final String CONFLICT_DETAIL = "A resource with the same identifying value already exists";
    static final String VALIDATION_DETAIL = "Provided data is invalid";
    static final String BEARER = "Bearer";

    @Override
    public Response toResponse(Throwable throwable) {
        ProblemDetail problem = toProblem(throwable);

        if (problem.status() >= 500) {
            logger.error(KeycloakErrorHandler.UNCAUGHT_SERVER_ERROR_TEXT, throwable);
        } else {
            logger.debugv(throwable, KeycloakErrorHandler.ERROR_RESPONSE_TEXT, problem.status());
        }

        String body;
        try {
            body = JsonSerialization.writeValueAsString(problem);
        } catch (IOException e) {
            // a record of strings and numbers cannot fail to serialize, but never fail while reporting a failure
            logger.error("Failed to serialize the problem details", e);
            return Response.serverError().build();
        }

        Response.ResponseBuilder response = Response.status(problem.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(body);
        if (problem.status() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            // RFC 9110: a 401 response must carry a challenge
            response.header(HttpHeaders.WWW_AUTHENTICATE, BEARER);
        }
        if (problem.status() == Response.Status.METHOD_NOT_ALLOWED.getStatusCode() && throwable instanceof NotAllowedException e) {
            // RFC 9110 §15.5.6: a 405 response MUST include an Allow header; RESTEasy computes it from the annotations
            Set<String> allowedMethods = e.getResponse().getAllowedMethods();
            if (!allowedMethods.isEmpty()) {
                response.allow(allowedMethods);
            }
        }
        return response.build();
    }

    static ProblemDetail toProblem(Throwable throwable) {
        if (throwable instanceof ServiceException e) {
            if (e.getViolations() != null) {
                return validationFailed(e.getViolations());
            }
            return of(e.getType(), e.getType() == ProblemType.INTERNAL_ERROR ? INTERNAL_ERROR_DETAIL : e.getMessage());
        }
        if (throwable instanceof ConstraintViolationException e) {
            return validationFailed(e);
        }
        if (throwable instanceof org.keycloak.scim.protocol.ForbiddenException) {
            return of(ProblemType.FORBIDDEN, null);
        }
        if (throwable instanceof UnrecognizedPropertyException e) {
            return unknownField(e);
        }
        if (throwable.getCause() instanceof UnrecognizedPropertyException e) {
            return unknownField(e);
        }
        if (throwable instanceof JsonProcessingException || throwable.getCause() instanceof JsonProcessingException) {
            return of(ProblemType.MALFORMED_BODY, MALFORMED_BODY_DETAIL);
        }
        if (throwable instanceof ModelDuplicateException || throwable.getCause() instanceof ModelDuplicateException) {
            return of(ProblemType.CONFLICT, CONFLICT_DETAIL);
        }
        if (throwable instanceof ModelValidationException e) {
            return of(ProblemType.VALIDATION_FAILED, e.getMessage());
        }
        if (throwable.getCause() instanceof ModelValidationException e) {
            return of(ProblemType.VALIDATION_FAILED, e.getMessage());
        }
        if (throwable instanceof WebApplicationException e) {
            return fromStatus(e);
        }
        return of(ProblemType.INTERNAL_ERROR, INTERNAL_ERROR_DETAIL);
    }

    private static ProblemDetail validationFailed(ConstraintViolationException exception) {
        return validationFailed(exception.getConstraintViolations().stream()
                .map(ProblemDetailErrorFormatter::toViolation)
                .sorted(Comparator.comparing(ProblemDetail.Violation::field)
                        .thenComparing(ProblemDetail.Violation::message))
                .toList());
    }

    private static ProblemDetail validationFailed(List<ProblemDetail.Violation> violations) {
        return new ProblemDetail(ProblemType.VALIDATION_FAILED.getType(), ProblemType.VALIDATION_FAILED.getTitle(),
                ProblemType.VALIDATION_FAILED.getStatus().getStatusCode(), VALIDATION_DETAIL, violations);
    }

    private static ProblemDetail unknownField(UnrecognizedPropertyException exception) {
        return validationFailed(List.of(new ProblemDetail.Violation(exception.getPropertyName(), "unknown field")));
    }

    private static ProblemDetail.Violation toViolation(ConstraintViolation<?> violation) {
        return new ProblemDetail.Violation(violation.getPropertyPath().toString(), violation.getMessage());
    }

    /**
     * Exceptions raised by the framework or by the v1 helpers: the status is the only reliable information, and the
     * message is not used.
     */
    private static ProblemDetail fromStatus(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        if (status == Response.Status.BAD_REQUEST.getStatusCode()) {
            return of(ProblemType.MALFORMED_BODY, MALFORMED_BODY_DETAIL);
        }
        if (status == Response.Status.NOT_FOUND.getStatusCode() && isParameterConversionFailure(exception)) {
            // the framework answers 404 when a query parameter cannot be converted, for example offset=abc
            return of(ProblemType.VALIDATION_FAILED, "offset and limit must be integers");
        }
        for (ProblemType type : List.of(ProblemType.UNAUTHENTICATED, ProblemType.FORBIDDEN, ProblemType.NOT_FOUND,
                ProblemType.METHOD_NOT_ALLOWED, ProblemType.CONFLICT, ProblemType.UNSUPPORTED_MEDIA_TYPE,
                ProblemType.INTERNAL_ERROR)) {
            if (type.getStatus().getStatusCode() == status) {
                return of(type, status >= 500 ? INTERNAL_ERROR_DETAIL : null);
            }
        }
        Response.Status known = Response.Status.fromStatusCode(status);
        String title = known != null ? known.getReasonPhrase() : "HTTP " + status;
        return new ProblemDetail(ProblemType.fallbackType(status), title, status, status >= 500 ? INTERNAL_ERROR_DETAIL : null, null);
    }

    private static boolean isParameterConversionFailure(WebApplicationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof NumberFormatException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static ProblemDetail of(ProblemType type, String detail) {
        return new ProblemDetail(type.getType(), type.getTitle(), type.getStatus().getStatusCode(), detail, null);
    }

}
