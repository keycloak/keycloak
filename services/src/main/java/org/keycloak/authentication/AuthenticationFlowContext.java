package org.keycloak.authentication;

import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import java.net.URI;

/**
 * This interface encapsulates information about an execution in an AuthenticationFlow.  It is also used to set
 * the status of the execution being performed.
 *
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AuthenticationFlowContext extends AbstractAuthenticationFlowContext {

    /**
     * Current user attached to this flow.  It can return null if no user has been identified yet
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
     * ClientSessionModel attached to this flow
     *
     * @return
     */
    ClientSessionModel getClientSession();

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
