package org.keycloak.services.error;

import jakarta.ws.rs.core.Response;

/**
 * Builds the error response for requests to the Admin API v2 ({@code /admin/api/**}).
 * <p>
 * {@link KeycloakErrorHandler} is registered globally and also serves the v1 admin API and the protocol endpoints,
 * and the Admin API v2 lives in a module that this module cannot depend on. An implementation is therefore looked up
 * with {@link java.util.ServiceLoader} and used by {@link KeycloakErrorHandler} for requests below {@code /admin/api/}.
 */
public interface AdminApiV2ErrorFormatter {

    /**
     * Converts the throwable into the response sent to the client.
     * The implementation is responsible for logging the throwable.
     * The transaction has already been marked for rollback by the caller.
     */
    Response toResponse(Throwable throwable);

}
