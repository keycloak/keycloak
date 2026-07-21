package org.keycloak.services;

import java.util.Optional;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ErrorRepresentation;
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
        Response response = KeycloakErrorHandler.getResponse(session, exception.toWebApplicationException());
        Optional<Object[]> parameters = exception.getParameters();
        if (exception.getMessage() != null && parameters.isPresent()
                && response.getMediaType() != null && MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType())) {
            ErrorRepresentation error = new ErrorRepresentation();
            error.setErrorMessage(exception.getMessage());
            error.setParams(parameters.get());
            return Response.fromResponse(response).entity(error).build();
        }
        return response;
    }
}
