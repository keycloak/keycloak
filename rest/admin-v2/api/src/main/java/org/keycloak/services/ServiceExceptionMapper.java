package org.keycloak.services;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.error.KeycloakErrorHandler;

/**
 * Exception mapper from {@link ServiceException} to {@link jakarta.ws.rs.WebApplicationException}.
 * <p>
 * Useful for mapping exceptions from the service layer to JAX-RS responses.
 */
@Provider
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {

    @Context
    KeycloakSession session;

    @Override
    public Response toResponse(ServiceException exception) {
        return KeycloakErrorHandler.getResponse(session, exception.toWebApplicationException());
    }
}
