package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.BruteForceProtector;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface AuthenticatorContext {
    EventBuilder getEvent();

    AuthenticationExecutionModel getExecution();

    void setExecution(AuthenticationExecutionModel execution);

    AuthenticatorModel getAuthenticatorModel();

    void setAuthenticatorModel(AuthenticatorModel model);

    String getAction();

    Authenticator getAuthenticator();

    void setAuthenticator(Authenticator authenticator);

    AuthenticationProcessor.Status getStatus();

    UserModel getUser();

    void setUser(UserModel user);

    RealmModel getRealm();

    ClientSessionModel getClientSession();
    void attachUserSession(UserSessionModel userSession);

    ClientConnection getConnection();

    UriInfo getUriInfo();

    KeycloakSession getSession();

    HttpRequest getHttpRequest();
    BruteForceProtector getProtector();

    void success();
    void failure(AuthenticationProcessor.Error error);
    void failure(AuthenticationProcessor.Error error, Response response);
    void challenge(Response challenge);

    void forceChallenge(Response challenge);

    void failureChallenge(AuthenticationProcessor.Error error, Response challenge);
    void attempted();

    /**
     * This could be an error message forwarded from brokering when the broker failed authentication
     * and we want to continue authentication locally.  forwardedErrorMessage can then be displayed by
     * whatever form is challenging.
     */
    String getForwardedErrorMessage();

    /**
     * Generates access code and updates clientsession timestamp
     *
     * @return
     */
    String generateAccessCode();
}
