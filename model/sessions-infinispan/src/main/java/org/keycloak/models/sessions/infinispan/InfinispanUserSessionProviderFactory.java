package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.Version;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.compat.MemUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.compat.SimpleUserSessionInitializer;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.initializer.InfinispanUserSessionInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflineUserSessionLoader;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * Uses Infinispan to store user sessions. On EAP 6.4 (Infinispan 5.2) map reduce is not supported for local caches as a work around
 * the old memory user session provider is used in this case. This can be removed once we drop support for EAP 6.4.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    private Config.Scope config;
    private Boolean compatMode;
    private MemUserSessionProviderFactory compatProviderFactory;

    @Override
    public UserSessionProvider create(KeycloakSession session) {

        if (!compatMode) {
            InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
            Cache<String, SessionEntity> offlineSessionsCache = connections.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);
            Cache<LoginFailureKey, LoginFailureEntity> loginFailures = connections.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
            return new InfinispanUserSessionProvider(session, cache, offlineSessionsCache, loginFailures);
        } else {
            return compatProviderFactory.create(session);
        }
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(final KeycloakSessionFactory factory) {
        KeycloakModelUtils.runJobInTransaction(factory, new KeycloakSessionTask() {

            @Override
            public void run(KeycloakSession session) {
                compatMode = isCompatMode(session);
                if (compatMode) {
                    compatProviderFactory = new MemUserSessionProviderFactory();
                }
            }

        });

        // Max count of worker errors. Initialization will end with exception when this number is reached
        final int maxErrors = config.getInt("maxErrors", 20);

        // Count of sessions to be computed in each segment
        final int sessionsPerSegment = config.getInt("sessionsPerSegment", 100);

        factory.register(new ProviderEventListener() {

            @Override
            public void onEvent(ProviderEvent event) {
                if (event instanceof PostMigrationEvent) {
                    loadPersistentSessions(factory, maxErrors, sessionsPerSegment);
                }
            }
        });
    }


    @Override
    public void loadPersistentSessions(final KeycloakSessionFactory sessionFactory, final int maxErrors, final int sessionsPerSegment) {
        log.debug("Start pre-loading userSessions and clientSessions from persistent storage");

        if (compatMode) {
            SimpleUserSessionInitializer initializer = new SimpleUserSessionInitializer(sessionFactory, new OfflineUserSessionLoader(), sessionsPerSegment);
            initializer.loadPersistentSessions();

        } else {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession session) {
                    InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
                    Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME);

                    InfinispanUserSessionInitializer initializer = new InfinispanUserSessionInitializer(sessionFactory, cache, new OfflineUserSessionLoader(), maxErrors, sessionsPerSegment, "offlineUserSessions");
                    initializer.initCache();
                    initializer.loadPersistentSessions();
                }

            });
        }

        log.debug("Pre-loading userSessions and clientSessions from persistent storage finished");
    }

    @Override
    public void close() {
        if (compatProviderFactory != null) {
            compatProviderFactory.close();
        }
    }

    @Override
    public String getId() {
        return "infinispan";
    }

    private boolean isCompatMode(KeycloakSession session) {
        // For unit tests
        if (this.config.getBoolean("enforceCompat", false)) {
            log.info("Enforced compatibility mode for infinispan. Falling back to deprecated mem user session provider.");
            return true;
        }

        if (Version.getVersionShort() < Version.getVersionShort("5.3.0.Final")) {
            InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
            if (cache.getAdvancedCache().getRpcManager() == null) {
                log.info("Infinispan version doesn't support map reduce for local cache. Falling back to deprecated mem user session provider.");
                return true;
            }
        }
        return false;
    }

}

