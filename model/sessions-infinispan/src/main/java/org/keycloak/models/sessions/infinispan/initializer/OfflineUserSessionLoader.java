package org.keycloak.models.sessions.infinispan.initializer;

import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OfflineUserSessionLoader implements SessionLoader {

    private static final Logger log = Logger.getLogger(OfflineUserSessionLoader.class);

    @Override
    public void init(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        int startTime = (int)(session.getKeycloakSessionFactory().getServerStartupTimestamp() / 1000);

        log.debugf("Clearing detached sessions from persistent storage and updating timestamps to %d", startTime);

        persister.clearDetachedUserSessions();
        persister.updateAllTimestamps(startTime);
    }

    @Override
    public int getSessionsCount(KeycloakSession session) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        return persister.getUserSessionsCount(true);
    }

    @Override
    public boolean loadSessions(KeycloakSession session, int first, int max) {
        if (log.isTraceEnabled()) {
            log.tracef("Loading sessions - first: %d, max: %d", first, max);
        }

        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        List<UserSessionModel> sessions = persister.loadUserSessions(first, max, true);

        for (UserSessionModel persistentSession : sessions) {

            // Save to memory/infinispan
            UserSessionModel offlineUserSession = session.sessions().importUserSession(persistentSession, true);

            for (ClientSessionModel persistentClientSession : persistentSession.getClientSessions()) {
                ClientSessionModel offlineClientSession = session.sessions().importClientSession(persistentClientSession, true);
                offlineClientSession.setUserSession(offlineUserSession);
            }
        }

        return true;
    }


}
