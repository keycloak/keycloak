package org.keycloak.services.managers;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;

import javax.ws.rs.ForbiddenException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
* @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
*/
public class Auth {
    private final boolean cookie;
    private final RealmModel realm;
    private final AccessToken token;
    private final UserModel user;
    private final UserModel client;

    public Auth(RealmModel realm, UserModel user, UserModel client) {
        this.cookie = true;
        this.realm = realm;
        this.token = null;

        this.user = user;
        this.client = client;
    }

    public Auth(AccessToken token, UserModel user, UserModel client) {
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

    public UserModel getClient() {
        return client;
    }

    public AccessToken getToken() {
        return token;
    }

    public void require(String role) {
        if (!has(role)) {
            throw new ForbiddenException();
        }
    }

    public void require(String app, String role) {
        if (!has(app, role)) {
            throw new ForbiddenException();
        }
    }

    public void require(ApplicationModel app, String role) {
        if (!has(app, role)) {
            throw new ForbiddenException();
        }
    }

    public void requireOneOf(String app, String... roles) {
        if(!hasOneOf(app, roles)) {
            throw new ForbiddenException();
        }
    }

    public void requireOneOf(ApplicationModel app, String... roles) {
        if(!hasOneOf(app, roles)) {
            throw new ForbiddenException();
        }
    }

    public boolean has(String role) {
        if (cookie)  {
            return realm.hasRole(user, realm.getRole(role));
        } else {
            return token.getRealmAccess() != null && token.getRealmAccess().isUserInRole(role);
        }
    }

    public boolean has(String app, String role) {
        if (cookie) {
            return realm.hasRole(user, realm.getApplicationByName(app).getRole(role));
        } else {
            AccessToken.Access access = token.getResourceAccess(app);
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean has(ApplicationModel app, String role) {
        if (cookie) {
            return realm.hasRole(user, app.getRole(role));
        } else {
            AccessToken.Access access = token.getResourceAccess(app.getName());
            return access != null && access.isUserInRole(role);
        }
    }

    public boolean hasOneOf(String app, String... roles) {
        for (String r : roles) {
            if (has(app, r)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOneOf(ApplicationModel app, String... roles) {
        for (String r : roles) {
            if (has(app, r)) {
                return true;
            }
        }
        return false;
    }
}
