package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.BruteForceProtector;

import javax.ws.rs.core.UriInfo;

/**
 * Interface that encapsulates the current state of the current form being executed
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface FormContext {
    /**
     * Current event builder being used
     *
     * @return
     */
    EventBuilder getEvent();

    /**
     * Create a refresh new EventBuilder to use within this context
     *
     * @return
     */
    EventBuilder newEvent();

    /**
     * The current execution in the flow
     *
     * @return
     */
    AuthenticationExecutionModel getExecution();

    /**
     * Current user attached to this flow.  It can return null if no uesr has been identified yet
     *
     * @return
     */
    UserModel getUser();

    /**
     * Attach a specific user to this flow.
     *
     * @param user
     */
    void setUser(UserModel user);

    /**
     * Current realm
     *
     * @return
     */
    RealmModel getRealm();

    /**
     * ClientSessionModel attached to this flow
     *
     * @return
     */
    ClientSessionModel getClientSession();

    /**
     * Information about the IP address from the connecting HTTP client.
     *
     * @return
     */
    ClientConnection getConnection();

    /**
     * UriInfo of the current request
     *
     * @return
     */
    UriInfo getUriInfo();

    /**
     * Current session
     *
     * @return
     */
    KeycloakSession getSession();

    HttpRequest getHttpRequest();

    /**
     * Get any configuration associated with the current execution
     *
     * @return
     */
    AuthenticatorConfigModel getAuthenticatorConfig();
}
