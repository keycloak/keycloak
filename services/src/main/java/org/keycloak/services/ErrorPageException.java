package org.keycloak.services;

import org.keycloak.models.KeycloakSession;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorPageException extends WebApplicationException {

    private final KeycloakSession session;
    private final String errorMessage;
    private final Object[] parameters;

    public ErrorPageException(KeycloakSession session, String errorMessage, Object... parameters) {
        this.session = session;
        this.errorMessage = errorMessage;
        this.parameters = parameters;
    }

    @Override
    public Response getResponse() {
        return ErrorPage.error(session, errorMessage, parameters);
    }

}
