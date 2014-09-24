package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private DefaultCacheManager cacheManager;
    private Cache<String, UserSessionEntity> userSessions;
    private Cache<String, ClientSessionEntity> clientSessions;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        return new InfinispanUserSessionProvider(session, userSessions, clientSessions);
    }

    @Override
    public void init(Config.Scope config) {
        Configuration configuration = new ConfigurationBuilder().indexing().enable().indexLocalOnly(true).build();

        cacheManager = new DefaultCacheManager(configuration);
        userSessions = cacheManager.getCache("userSessions");
        clientSessions = cacheManager.getCache("clientSessions");
    }

    @Override
    public void close() {
        if (cacheManager != null) {
            cacheManager.stop();
            cacheManager = null;
        }
    }

    @Override
    public String getId() {
        return "infinispan";
    }

}
