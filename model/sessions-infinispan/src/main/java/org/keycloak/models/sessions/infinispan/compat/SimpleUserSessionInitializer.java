package org.keycloak.models.sessions.infinispan.compat;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.sessions.infinispan.initializer.SessionLoader;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SimpleUserSessionInitializer {

    private final KeycloakSessionFactory sessionFactory;
    private final SessionLoader sessionLoader;
    private final int sessionsPerSegment;

    public SimpleUserSessionInitializer(KeycloakSessionFactory sessionFactory, SessionLoader sessionLoader, int sessionsPerSegment) {
        this.sessionFactory = sessionFactory;
        this.sessionLoader = sessionLoader;
        this.sessionsPerSegment = sessionsPerSegment;
    }

    public void loadPersistentSessions() {
        // Rather use separate transactions for update and loading

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                sessionLoader.init(session);
            }

        });

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                int count = sessionLoader.getSessionsCount(session);

                for (int i=0 ; i<count ; i+=sessionsPerSegment) {
                    sessionLoader.loadSessions(session, i, sessionsPerSegment);
                }
            }

        });
    }
}
