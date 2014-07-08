package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredUserSessions implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        for (RealmModel realm : session.getModel().getRealms()) {
            realm.removeExpiredUserSessions();
        }
    }

}
