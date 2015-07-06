package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClearExpiredUserSessions implements ScheduledTask {

    private static final Logger log = Logger.getLogger(ClearExpiredUserSessions.class);

    @Override
    public void run(KeycloakSession session) {
        UserSessionProvider sessions = session.sessions();
        long start = System.currentTimeMillis();
        for (RealmModel realm : session.realms().getRealms()) {
            sessions.removeExpiredUserSessions(realm);
        }
        log.warnf("Removed expired user sessions in %d ms", System.currentTimeMillis() - start);
    }

}
