package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

import javax.ws.rs.core.UriInfo;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RequiredActionContext {
    EventBuilder getEvent();
    UserModel getUser();
    RealmModel getRealm();
    ClientSessionModel getClientSession();
    UserSessionModel getUserSession();
    ClientConnection getConnection();
    UriInfo getUriInfo();
    KeycloakSession getSession();
    HttpRequest getHttpRequest();
    String generateAccessCode(String action);
}
