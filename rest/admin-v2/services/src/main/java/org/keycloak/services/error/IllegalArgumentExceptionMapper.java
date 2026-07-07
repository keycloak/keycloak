package org.keycloak.services.error;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.keycloak.models.KeycloakSession;

/**
 * Maps uncaught {@link IllegalArgumentException} to HTTP 400 for Admin API v2 requests.
 * <p>
 * Other endpoints continue to surface {@link IllegalArgumentException} as HTTP 500 via {@link KeycloakErrorHandler}.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    static final String ADMIN_API_V2_PATH = "/admin/api/";

    @Context
    KeycloakSession session;

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        if (!isAdminApiV2Request(session)) {
            return KeycloakErrorHandler.getResponse(session, exception);
        }
        return KeycloakErrorHandler.getResponse(session, new BadRequestException(exception.getMessage(), exception));
    }

    static boolean isAdminApiV2Request(KeycloakSession session) {
        return session.getContext().getHttpRequest().getUri().getPath().contains(ADMIN_API_V2_PATH);
    }
}
