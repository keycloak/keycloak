package org.keycloak.services.error;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.services.ServiceException;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProblemDetailErrorFormatterTest {

    /**
     * A real validator runs against this bean, so the violations are exactly what Hibernate Validator produces.
     */
    static class Payload {
        @NotBlank
        String clientId;

        @NotNull
        List<@NotBlank String> redirectUris;

        Payload(String clientId, List<String> redirectUris) {
            this.clientId = clientId;
            this.redirectUris = redirectUris;
        }
    }

    private static final Validator VALIDATOR = Validation.byProvider(HibernateValidator.class).configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory().getValidator();

    private final ProblemDetailErrorFormatter formatter = new ProblemDetailErrorFormatter();

    private static ConstraintViolationException invalidPayload() {
        return new ConstraintViolationException(VALIDATOR.validate(new Payload("", List.of(""))));
    }

    @Test
    void everyTypeHasAUniqueUrnAndTitle() {
        Set<String> types = new HashSet<>();
        for (ProblemType type : ProblemType.values()) {
            assertTrue(type.getType().startsWith("urn:keycloak:admin-api:v2:problem:"), type.name());
            assertTrue(types.add(type.getType()), "duplicate type " + type.getType());
            assertFalse(type.getTitle().isBlank(), type.name());
        }
    }

    @Test
    void serviceExceptionUsesItsTypeAndMessage() {
        ProblemDetail problem = ProblemDetailErrorFormatter
                .toProblem(new ServiceException(ProblemType.CONFLICT, "uuid already exists, but with a different clientId"));

        assertEquals(ProblemType.CONFLICT.getType(), problem.type());
        assertEquals("Conflict", problem.title());
        assertEquals(409, problem.status());
        assertEquals("uuid already exists, but with a different clientId", problem.detail());
        assertNull(problem.violations());
    }

    @Test
    void serviceExceptionWithViolationsIsAValidationFailure() {
        ProblemDetail problem = ProblemDetailErrorFormatter
                .toProblem(ServiceException.invalid("sort", "sort direction must be asc or desc"));

        assertEquals(ProblemType.VALIDATION_FAILED.getType(), problem.type());
        assertEquals(400, problem.status());
        assertEquals(ProblemDetailErrorFormatter.VALIDATION_DETAIL, problem.detail());
        assertEquals(List.of(new ProblemDetail.Violation("sort", "sort direction must be asc or desc")), problem.violations());
    }

    @Test
    void serviceExceptionOfServerErrorDoesNotExposeItsMessage() {
        ProblemDetail problem = ProblemDetailErrorFormatter
                .toProblem(new ServiceException(ProblemType.INTERNAL_ERROR, "Cannot find service account user"));

        assertEquals(500, problem.status());
        assertEquals(ProblemDetailErrorFormatter.INTERNAL_ERROR_DETAIL, problem.detail());
    }

    @Test
    void constraintViolationsBecomeFieldAndMessage() {
        ProblemDetail problem = ProblemDetailErrorFormatter.toProblem(invalidPayload());

        assertEquals(ProblemType.VALIDATION_FAILED.getType(), problem.type());
        assertEquals(400, problem.status());
        assertEquals(ProblemDetailErrorFormatter.VALIDATION_DETAIL, problem.detail());
        assertEquals(2, problem.violations().size());
        // sorted, so that the response is stable
        assertEquals(new ProblemDetail.Violation("clientId", "must not be blank"), problem.violations().get(0));
        assertEquals(new ProblemDetail.Violation("redirectUris[0].<list element>", "must not be blank"), problem.violations().get(1));
    }

    @Test
    void frameworkExceptionsAreClassifiedByStatusAndTheirMessageIsNotCopied() {
        assertFramework(new NotFoundException("Unable to find matching target resource method"), ProblemType.NOT_FOUND);
        assertFramework(new NotAuthorizedException("Bearer token format error", "Bearer"), ProblemType.UNAUTHENTICATED);
        assertFramework(new ForbiddenException("secret internals"), ProblemType.FORBIDDEN);
        assertFramework(new NotAllowedException("GET", new String[0]), ProblemType.METHOD_NOT_ALLOWED);
        assertFramework(new NotSupportedException("No supported MessageBodyReader found"), ProblemType.UNSUPPORTED_MEDIA_TYPE);
        assertFramework(new WebApplicationException("secret internals", Response.Status.CONFLICT), ProblemType.CONFLICT);
    }

    private void assertFramework(Throwable throwable, ProblemType expected) {
        ProblemDetail problem = ProblemDetailErrorFormatter.toProblem(throwable);
        assertEquals(expected.getType(), problem.type());
        assertEquals(expected.getStatus().getStatusCode(), problem.status());
        assertNull(problem.detail(), "the framework message must not be copied: " + problem.detail());
    }

    @Test
    void statusWithoutACatalogueEntryUsesTheFallbackType() {
        ProblemDetail problem = ProblemDetailErrorFormatter.toProblem(new NotAcceptableException());

        assertEquals("urn:keycloak:admin-api:v2:problem:http-406", problem.type());
        assertEquals("Not Acceptable", problem.title());
        assertEquals(406, problem.status());
    }

    @Test
    void nonNumericQueryParameterIsAValidationFailureNotA404() {
        ProblemDetail problem = ProblemDetailErrorFormatter
                .toProblem(new NotFoundException(new NumberFormatException("For input string: \"abc\"")));

        assertEquals(ProblemType.VALIDATION_FAILED.getType(), problem.type());
        assertEquals(400, problem.status());
    }

    @Test
    void modelExceptions() {
        ProblemDetail duplicate = ProblemDetailErrorFormatter.toProblem(new ModelDuplicateException("Duplicate resource error"));
        assertEquals(ProblemType.CONFLICT.getType(), duplicate.type());
        assertEquals(409, duplicate.status());
        assertFalse(duplicate.detail().contains("Duplicate resource error"));

        ProblemDetail invalid = ProblemDetailErrorFormatter.toProblem(new ModelValidationException("bad value"));
        assertEquals(ProblemType.VALIDATION_FAILED.getType(), invalid.type());
        assertEquals(400, invalid.status());
    }

    @Test
    void jsonErrorsDoNotEchoTheParserMessage() {
        JsonProcessingException cause = new JsonParseException(null, "Unexpected character ('x' (code 120))");

        ProblemDetail direct = ProblemDetailErrorFormatter.toProblem(cause);
        ProblemDetail wrapped = ProblemDetailErrorFormatter.toProblem(new RuntimeException(cause));

        for (ProblemDetail problem : Arrays.asList(direct, wrapped)) {
            assertEquals(ProblemType.MALFORMED_BODY.getType(), problem.type());
            assertEquals(400, problem.status());
            assertEquals(ProblemDetailErrorFormatter.MALFORMED_BODY_DETAIL, problem.detail());
        }
    }

    @Test
    void unexpectedExceptionsAreInternalErrorsWithoutTheirText() {
        for (Throwable throwable : Arrays.asList(new NullPointerException("secret.field is null"),
                new IllegalStateException("jdbc:postgresql://db/keycloak"), new InternalServerErrorException("boom"))) {
            ProblemDetail problem = ProblemDetailErrorFormatter.toProblem(throwable);

            assertEquals(ProblemType.INTERNAL_ERROR.getType(), problem.type());
            assertEquals(500, problem.status());
            assertEquals(ProblemDetailErrorFormatter.INTERNAL_ERROR_DETAIL, problem.detail());
        }
    }

    @Test
    void responseIsProblemJsonAndOmitsAbsentMembers() throws Exception {
        Response response = formatter.toResponse(new ServiceException(ProblemType.NOT_FOUND, "Cannot find the specified client"));

        assertEquals(404, response.getStatus());
        assertEquals("application/problem+json", response.getMediaType().toString());
        var body = JsonSerialization.mapper.readTree((String) response.getEntity());
        assertEquals("urn:keycloak:admin-api:v2:problem:not-found", body.get("type").asText());
        assertEquals("Not found", body.get("title").asText());
        assertEquals(404, body.get("status").asInt());
        assertEquals("Cannot find the specified client", body.get("detail").asText());
        assertFalse(body.has("violations"));
        assertFalse(body.has("error"));
        assertNull(response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void unauthorizedResponseCarriesAChallenge() {
        Response response = formatter.toResponse(new NotAuthorizedException("Bearer"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void methodNotAllowedResponseCarriesAllowHeader() {
        Response response = formatter.toResponse(new NotAllowedException("GET", new String[]{"POST"}));

        assertEquals(405, response.getStatus());
        Set<String> allowed = response.getAllowedMethods();
        assertTrue(allowed.contains("GET"), "Allow header must contain GET");
        assertTrue(allowed.contains("POST"), "Allow header must contain POST");
    }

    @Test
    void validationResponseSerializesViolationsAsObjects() throws Exception {
        Response response = formatter.toResponse(invalidPayload());

        var body = JsonSerialization.mapper.readTree((String) response.getEntity());
        assertNotNull(body.get("violations"));
        assertEquals("clientId", body.get("violations").get(0).get("field").asText());
        assertEquals("must not be blank", body.get("violations").get(0).get("message").asText());
    }

}
