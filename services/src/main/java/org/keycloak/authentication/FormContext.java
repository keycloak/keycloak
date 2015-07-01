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

import javax.ws.rs.core.UriInfo;

/**
* @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
* @version $Revision: 1 $
*/
public interface FormContext {
    EventBuilder getEvent();
    EventBuilder newEvent();
    AuthenticationExecutionModel getExecution();
    UserModel getUser();
    void setUser(UserModel user);
    RealmModel getRealm();
    ClientSessionModel getClientSession();
    ClientConnection getConnection();
    UriInfo getUriInfo();
    KeycloakSession getSession();
    HttpRequest getHttpRequest();
    AuthenticatorConfigModel getAuthenticatorConfig();

}
