package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.Version;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.compat.MemUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * Uses Infinispan to store user sessions. On EAP 6.4 (Infinispan 5.2) map reduce is not supported for local caches as a work around
 * the old memory user session provider is used in this case. This can be removed once we drop support for EAP 6.4.
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanUserSessionProviderFactory.class);

    private Boolean compatMode;
    private MemUserSessionProviderFactory compatProviderFactory;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        if (compatMode == null) {
            synchronized (this) {
                if (compatMode == null) {
                    compatMode = isCompatMode(session);
                    if (compatMode) {
                        compatProviderFactory = new MemUserSessionProviderFactory();
                        log.info("Infinispan version doesn't support map reduce for local cache. Falling back to deprecated mem user session provider.");
                    }
                }
            }
        }

        if (!compatMode) {
            InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
            Cache<LoginFailureKey, LoginFailureEntity> loginFailures = connections.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
            return new InfinispanUserSessionProvider(session, cache, loginFailures);
        } else {
            return compatProviderFactory.create(session);
        }
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

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

    private static boolean isCompatMode(KeycloakSession session) {
        if (Version.getVersionShort() < Version.getVersionShort("5.3.0.Final")) {
            InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
            if (cache.getAdvancedCache().getRpcManager() == null) {
                return true;
            }
        }
        return false;
    }

}

