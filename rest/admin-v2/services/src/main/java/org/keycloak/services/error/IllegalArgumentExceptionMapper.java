package org.keycloak.services.error;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.keycloak.models.KeycloakSession;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Maps uncaught {@link IllegalArgumentException} to HTTP 400 for Admin API v2 requests.
 * <p>
 * Declared with {@link ServerExceptionMapper} rather than a classic JAX-RS {@code @Provider}
 * {@code ExceptionMapper}, so this stays a mapper dedicated to Admin API v2 instead of adding another branch to the
 * global {@link KeycloakErrorHandler}. As mappers declared outside a resource class are still registered
 * application-wide, the request path is checked to keep the scope limited to Admin API v2.
 * <p>
 * Other endpoints continue to surface {@link IllegalArgumentException} as HTTP 500 via {@link KeycloakErrorHandler}.
 */
public class IllegalArgumentExceptionMapper {

    static final String ADMIN_API_V2_PATH = "/admin/api/";

    @Context
    KeycloakSession session;

    @ServerExceptionMapper
    public Response toResponse(IllegalArgumentException exception, UriInfo uriInfo) {
        if (!isAdminApiV2Request(uriInfo)) {
            return KeycloakErrorHandler.getResponse(session, exception);
        }
        return KeycloakErrorHandler.getResponse(session, new BadRequestException(exception.getMessage(), exception));
    }

    static boolean isAdminApiV2Request(UriInfo uriInfo) {
        return uriInfo.getPath().contains(ADMIN_API_V2_PATH);
    }
}
