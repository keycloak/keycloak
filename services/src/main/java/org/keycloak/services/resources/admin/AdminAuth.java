package org.keycloak.services.resources.admin;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminAuth {

    private final RealmModel realm;
    private final AccessToken token;
    private final UserModel user;
    private final ClientModel client;

    public AdminAuth(RealmModel realm, AccessToken token, UserModel user, ClientModel client) {
        this.token = token;
        this.realm = realm;

        this.user = user;
        this.client = client;
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
        if (client instanceof ApplicationModel) {
            RoleModel roleModel = realm.getRole(role);
            return user.hasRole(roleModel) && client.hasScope(roleModel);
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
        if (client instanceof ApplicationModel) {
            RoleModel roleModel = app.getRole(role);
            return user.hasRole(roleModel) && client.hasScope(roleModel);
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
