package org.keycloak.services;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.keycloak.models.KeycloakSession;
import org.keycloak.utils.KeycloakSessionUtil;

/**
 * Base class for all Keycloak error exceptions that should roll back the current transaction.
 *
 * <p>Calling {@link #getResponse()} with a non-null result (containing an entity) leads to RESTEasy
 * directly returning that response instead of propagating the exception to
 * {@code KeycloakErrorHandler.toResponse(Throwable)}, which would normally ensure rollback.
 * This base class ensures {@code setRollbackOnly()} is always called before the error response
 * is returned.</p>
 *
 * <p>Subclasses must override {@link #createErrorResponse()} instead of {@link #getResponse()}.</p>
 *
 * @see org.keycloak.services.error.KeycloakErrorHandler
 */
public abstract class RollbackWebApplicationException extends WebApplicationException {

    protected RollbackWebApplicationException(String message, Response.Status status) {
        super(message, status);
    }

    protected RollbackWebApplicationException(String message, Response response) {
        super(message, response);
    }

    protected RollbackWebApplicationException(Throwable cause, Response response) {
        super(cause, response);
    }

    @Override
    public final Response getResponse() {
        KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
        if (session != null) {
            session.getTransactionManager().setRollbackOnly();
        }
        return createErrorResponse();
    }

    protected Response getOriginalResponse() {
        return super.getResponse();
    }

    /**
     * Builds the error response. Override this instead of {@link #getResponse()}.
     */
    public Response createErrorResponse() {
        return getOriginalResponse();
    }
}
