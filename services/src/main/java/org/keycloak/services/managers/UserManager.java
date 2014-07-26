package org.keycloak.services.managers;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserManager {

    private KeycloakSession session;

    public UserManager(KeycloakSession session) {
        this.session = session;
    }

    public boolean removeUser(RealmModel realm, UserModel user) {
        if (session.users().removeUser(realm, user)) {
            UserSessionProvider sessions = session.sessions();
            if (sessions != null) {
                sessions.onUserRemoved(realm, user);
            }
            return true;
        }
        return false;
    }

}
