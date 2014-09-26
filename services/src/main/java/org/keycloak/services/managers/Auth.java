package org.keycloak.services.managers;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;

/**
* @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
*/
public class Auth {

    private final boolean cookie;
    private final RealmModel realm;
    private final AccessToken token;
    private final UserModel user;
    private final ClientModel client;
    private final UserSessionModel session;
    private ClientSessionModel clientSession;

    public Auth(RealmModel realm, AccessToken token, UserModel user, ClientModel client, UserSessionModel session, boolean cookie) {
        this.cookie = cookie;
        this.token = token;
        this.realm = realm;

        this.user = user;
        this.client = client;
        this.session = session;
    }

    public boolean isCookieAuthenticated() {
        return cookie;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public UserModel getUser() {
        return user;
    }

    public ClientModel getClient() {
        return client;
    }

    public AccessToken getToken() {
        return token;
    }

    public UserSessionModel getSession() {
        return session;
    }

    public ClientSessionModel getClientSession() {
        return clientSession;
    }

    public void setClientSession(ClientSessionModel clientSession) {
        this.clientSession = clientSession;
    }

    public boolean hasRealmRole(String role) {
        if (cookie) {
            return user.hasRole(realm.getRole(role));
        } else {
            AccessToken.Access access = token.getRealmAccess();
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean hasOneOfRealmRole(String... roles) {
        for (String r : roles) {
            if (hasRealmRole(r)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAppRole(ApplicationModel app, String role) {
        if (cookie) {
            return user.hasRole(app.getRole(role));
        } else {
            AccessToken.Access access = token.getResourceAccess(app.getName());
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean hasOneOfAppRole(ApplicationModel app, String... roles) {
        for (String r : roles) {
            if (hasAppRole(app, r)) {
                return true;
            }
        }
        return false;
    }

}
