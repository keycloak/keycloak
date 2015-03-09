package org.keycloak.services;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ErrorPageException extends WebApplicationException {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UriInfo uriInfo;
    private final String errorMessage;

    public ErrorPageException(KeycloakSession session, RealmModel realm, UriInfo uriInfo, String errorMessage) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.errorMessage = errorMessage;
    }

    @Override
    public Response getResponse() {
        return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, errorMessage);
    }

}
