package org.keycloak.services.managers;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
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

    public Auth(RealmModel realm, UserModel user, ClientModel client) {
        this.cookie = true;
        this.realm = realm;
        this.token = null;

        this.user = user;
        this.client = client;
    }

    public Auth(AccessToken token, UserModel user, ClientModel client) {
        this.cookie = false;
        this.token = token;
        this.realm = null;

        this.user = user;
        this.client = client;
    }

    public boolean isCookie() {
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

    public boolean hasRealmRole(String role) {
        if (cookie) {
            return realm.hasRole(user, realm.getRole(role));
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

    public boolean hasAppRole(String app, String role) {
        if (cookie) {
            return realm.hasRole(user, realm.getApplicationByName(app).getRole(role));
        } else {
            AccessToken.Access access = token.getResourceAccess(app);
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean hasOneOfAppRole(String app, String... roles) {
        for (String r : roles) {
            if (hasAppRole(app, r)) {
                return true;
            }
        }
        return false;
    }

}
