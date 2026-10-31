package org.keycloak.quarkus.runtime.integration.jaxrs;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.services.error.KeycloakErrorHandler;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;

@Provider
public class QuarkusJacksonExceptionHandler {

    @Context
    KeycloakSession session;

    @ServerExceptionMapper
    public Response jacksonException(JacksonException exception) {
        // The Jackson base type also covers response serialization failures. Quarkus's readers
        // wrap input failures in an explicit HTTP 400, except for mismatched input (handled below)
        // and invalid definitions (server errors). Do not infer bad input from the base type.
        return KeycloakErrorHandler.getResponse(session, exception);
    }

    @ServerExceptionMapper
    public Response mismatchedInput(MismatchedInputException exception) {
        return jsonError(exception, exception);
    }

    @ServerExceptionMapper
    public Response webApplication(WebApplicationException exception) {
        if (exception.getResponse().getStatus() == Response.Status.BAD_REQUEST.getStatusCode()
                && exception.getCause() instanceof JacksonException jacksonException) {
            return jsonError(exception, jacksonException);
        }
        return KeycloakErrorHandler.getResponse(session, exception);
    }

    private Response jsonError(Throwable exception, JacksonException jacksonException) {
        OAuth2ErrorRepresentation error = new OAuth2ErrorRepresentation();
        error.setError(jacksonException instanceof StreamReadException ? OAuthErrorException.INVALID_REQUEST : "unknown_error");
        error.setErrorDescription("Cannot parse the JSON");
        return KeycloakErrorHandler.getResponse(session, exception, Response.Status.BAD_REQUEST, error);
    }
}
