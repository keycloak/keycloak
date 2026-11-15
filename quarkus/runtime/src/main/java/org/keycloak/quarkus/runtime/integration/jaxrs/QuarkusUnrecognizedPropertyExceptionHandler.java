package org.keycloak.quarkus.runtime.integration.jaxrs;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.error.KeycloakErrorHandler;

import tools.jackson.databind.exc.UnrecognizedPropertyException;

@Provider
public class QuarkusUnrecognizedPropertyExceptionHandler implements ExceptionMapper<UnrecognizedPropertyException> {

    @Context
    KeycloakSession session;

    @Override
    public Response toResponse(UnrecognizedPropertyException exception) {
        String message = String.format("Invalid json representation for %s. Unrecognized field \"%s\" at line %s column %s.",
                exception.getReferringClass().getSimpleName(), exception.getPropertyName(),
                exception.getLocation().getLineNr(), exception.getLocation().getColumnNr());
        return KeycloakErrorHandler.getResponse(session, new BadRequestException(message));
    }
}
