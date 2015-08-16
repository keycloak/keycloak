package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.BruteForceProtector;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * This interface encapsulates information about an execution in an AuthenticationFlow.  It is also used to set
 * the status of the execution being performed.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AuthenticationFlowContext {
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

    void attachUserSession(UserSessionModel userSession);

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
    BruteForceProtector getProtector();


    /**
     * Get any configuration associated with the current execution
     *
     * @return
     */
    AuthenticatorConfigModel getAuthenticatorConfig();

    /**
     * This could be an error message forwarded from brokering when the broker failed authentication
     * and we want to continue authentication locally.  forwardedErrorMessage can then be displayed by
     * whatever form is challenging.
     */
    String getForwardedErrorMessage();

    /**
     * Generates access code and updates clientsession timestamp
     * Access codes must be included in form action callbacks as a query parameter.
     *
     * @return
     */
    String generateAccessCode();


    AuthenticationExecutionModel.Requirement getCategoryRequirementFromCurrentFlow(String authenticatorCategory);


    /**
     * Mark the current execution as successful.  The flow will then continue
     *
     */
    void success();

    /**
     * Aborts the current flow
     *
     * @param error
     */
    void failure(AuthenticationFlowError error);

    /**
     * Aborts the current flow.
     *
     * @param error
     * @param response Response that will be sent back to HTTP client
     */
    void failure(AuthenticationFlowError error, Response response);

    /**
     * Sends a challenge response back to the HTTP client.  If the current execution requirement is optional, this response will not be
     * sent.  If the current execution requirement is alternative, then this challenge will be sent if no other alternative
     * execution was successful.
     *
     * @param challenge
     */
    void challenge(Response challenge);

    /**
     * Sends the challenge back to the HTTP client irregardless of the current executionr equirement
     *
     * @param challenge
     */
    void forceChallenge(Response challenge);

    /**
     * Same behavior as challenge(), but the error count in brute force attack detection will be incremented.
     * For example, if a user enters in a bad password, the user is directed to try again, but Keycloak will keep track
     * of how many failures have happened.
     *
     * @param error
     * @param challenge
     */
    void failureChallenge(AuthenticationFlowError error, Response challenge);

    /**
     * There was no failure or challenge.  The authenticator was attempted, but not fulfilled.  If the current execution
     * requirement is alternative or optional, then this status is ignored by the flow.
     *
     */
    void attempted();

    /**
     * Get the current status of the current execution.
     *
     * @return may return null if not set yet.
     */
    FlowStatus getStatus();

    /**
     * Get the error condition of a failed execution.
     *
     * @return may return null if there was no error
     */
    AuthenticationFlowError getError();

    /**
     * Create a Freemarker form builder that presets the user, action URI, and a generated access code
     *
     * @return
     */
    LoginFormsProvider form();

    /**
     * Get the action URL for the required action.
     *
     * @param code client session access code
     * @return
     */
    URI getActionUrl(String code);

    /**
     * Get the action URL for the required action.  This auto-generates the access code.
     *
     * @return
     */
    URI getActionUrl();

    /**
     * End the flow and redirect browser based on protocol specific respones.  This should only be executed
     * in browser-based flows.
     *
     */
    void cancelLogin();

    /**
     * Abort the current flow and restart it using the realm's browser login
     *
     * @return
     */
    void resetBrowserLogin();
}
