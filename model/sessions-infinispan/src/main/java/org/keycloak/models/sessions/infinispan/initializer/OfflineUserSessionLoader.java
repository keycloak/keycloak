package org.keycloak.models.sessions.infinispan.initializer;

import java.util.List;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.util.Time;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineUserSessionLoader implements SessionLoader {

    @Override
    public int getSessionsCount(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.getUserSessionsCount(true);
    }

    @Override
    public boolean loadSessions(KeycloakSession session, int first, int max) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        List<UserSessionModel> sessions = persister.loadUserSessions(first, max, true);

        // TODO: Each worker may have different time. Improve if needed...
        int currentTime = Time.currentTime();

        for (UserSessionModel persistentSession : sessions) {

            // Update and persist lastSessionRefresh time
            persistentSession.setLastSessionRefresh(currentTime);
            persister.updateUserSession(persistentSession, true);

            // Save to memory/infinispan
            UserSessionModel offlineUserSession = session.sessions().createOfflineUserSession(persistentSession);

            for (ClientSessionModel persistentClientSession : persistentSession.getClientSessions()) {
                ClientSessionModel offlineClientSession = session.sessions().createOfflineClientSession(persistentClientSession);
                offlineClientSession.setUserSession(offlineUserSession);
            }
        }

        return true;
    }


}
