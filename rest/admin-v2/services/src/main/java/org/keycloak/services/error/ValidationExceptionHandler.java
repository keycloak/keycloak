package org.keycloak.services.error;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;

/**
 * Routes bean validation failures to {@link KeycloakErrorHandler}.
 * <p>
 * This mapper is needed because Quarkus registers its own mapper for the parent type
 * {@link jakarta.validation.ValidationException}, which would otherwise be chosen over the mapper for
 * {@link Throwable} and bypass the Admin API v2 error format.
 */
@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Context
    KeycloakSession session;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return KeycloakErrorHandler.getResponse(session, exception);
    }
}
