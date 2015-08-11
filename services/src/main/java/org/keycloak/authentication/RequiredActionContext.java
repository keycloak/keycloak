package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Interface that encapsulates current information about the current requred action
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionContext {
    void ignore();

    public static enum Status {
        CHALLENGE,
        SUCCESS,
        IGNORE,
        FAILURE
    }

    /**
     * Current event builder being used
     *
     * @return
     */
    EventBuilder getEvent();

    /**
     * Current user
     *
     * @return
     */
    UserModel getUser();
    RealmModel getRealm();
    ClientSessionModel getClientSession();
    UserSessionModel getUserSession();
    ClientConnection getConnection();
    UriInfo getUriInfo();
    KeycloakSession getSession();
    HttpRequest getHttpRequest();

    /**
     * Generates access code and updates clientsession timestamp
     * Access codes must be included in form action callbacks as a query parameter.
     *
     * @return
     */
    String generateAccessCode(String action);

    Status getStatus();

    void challenge(Response response);
    void failure();
    void success();
}
