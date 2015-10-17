package org.keycloak.models.sessions.infinispan.initializer;

import java.io.Serializable;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.distexec.DistributedCallable;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionInitializerWorker implements DistributedCallable<String, SessionEntity, InfinispanUserSessionInitializer.WorkerResult>, Serializable {

    private static final Logger log = Logger.getLogger(SessionInitializerWorker.class);

    private int segment;
    private int sessionsPerSegment;
    private SessionLoader sessionLoader;

    private transient Cache<String, SessionEntity> cache;

    public void setWorkerEnvironment(int segment, int sessionsPerSegment, SessionLoader sessionLoader) {
        this.segment = segment;
        this.sessionsPerSegment = sessionsPerSegment;
        this.sessionLoader = sessionLoader;
    }

    @Override
    public void setEnvironment(Cache<String, SessionEntity> cache, Set<String> inputKeys) {
        this.cache = cache;
    }

    @Override
    public InfinispanUserSessionInitializer.WorkerResult call() throws Exception {
        if (log.isTraceEnabled()) {
            log.tracef("Running computation for segment: %d", segment);
        }

        KeycloakSessionFactory sessionFactory = cache.getAdvancedCache().getComponentRegistry().getComponent(KeycloakSessionFactory.class);
        if (sessionFactory == null) {
            log.warnf("KeycloakSessionFactory not yet set in cache. Worker skipped");
            return InfinispanUserSessionInitializer.WorkerResult.create(segment, false);
        }

        final int first = segment * sessionsPerSegment;
        final int max = sessionsPerSegment;

        KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                sessionLoader.loadSessions(session, first, max);
            }

        });

        return InfinispanUserSessionInitializer.WorkerResult.create(segment, true);
    }

}
