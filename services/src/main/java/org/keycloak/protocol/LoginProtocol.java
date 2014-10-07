package org.keycloak.protocol;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OpenIDConnect;
import org.keycloak.provider.Provider;
import org.keycloak.services.managers.ClientSessionCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface LoginProtocol extends Provider {
    LoginProtocol setSession(KeycloakSession session);

    LoginProtocol setRealm(RealmModel realm);

    LoginProtocol setUriInfo(UriInfo uriInfo);

    Response cancelLogin(ClientSessionModel clientSession);
    Response invalidSessionError(ClientSessionModel clientSession);
    Response authenticated(UserSessionModel userSession, ClientSessionCode accessCode);
    Response consentDenied(ClientSessionModel clientSession);

    void backchannelLogout(UserSessionModel userSession, ClientSessionModel clientSession);
}
