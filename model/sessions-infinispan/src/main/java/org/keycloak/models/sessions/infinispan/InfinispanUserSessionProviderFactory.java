package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    private DefaultCacheManager cacheManager;

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        return new InfinispanUserSessionProvider(session, cacheManager);
    }

    @Override
    public void init(Config.Scope config) {
        Configuration configuration = new ConfigurationBuilder().build();
        cacheManager = new DefaultCacheManager(configuration);
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
