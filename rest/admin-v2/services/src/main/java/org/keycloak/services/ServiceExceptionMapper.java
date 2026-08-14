package org.keycloak.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
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
            return Response.fromResponse(response)
                    .entity(addParameters(response.getEntity(), exception.getMessage(), parameters.get()))
                    .build();
        }
        return response;
    }

    static Object addParameters(Object entity, String message, Object[] parameters) {
        Object[] stableParameters = parameters.clone();
        if (entity instanceof ErrorRepresentation error) {
            if (error.getErrorMessage() == null) {
                error.setErrorMessage(message);
            }
            error.setParams(stableParameters);
            return error;
        }
        if (entity instanceof OAuth2ErrorRepresentation error) {
            Map<String, Object> enriched = new LinkedHashMap<>();
            enriched.put("error", error.getError());
            if (error.getErrorDescription() != null) {
                enriched.put("error_description", error.getErrorDescription());
            }
            enriched.put("errorMessage", message);
            enriched.put("params", stableParameters);
            return enriched;
        }

        ErrorRepresentation error = new ErrorRepresentation();
        error.setErrorMessage(message);
        error.setParams(stableParameters);
        return error;
    }
}
