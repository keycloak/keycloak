package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredUserSessions implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        UserSessionProvider sessions = session.sessions();
        for (RealmModel realm : session.realms().getRealms()) {
            sessions.removeExpiredUserSessions(realm);
        }
    }

}
