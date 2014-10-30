package org.keycloak.models.sessions.infinispan;

import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.UserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserSessionProviderFactory implements UserSessionProviderFactory {

    @Override
    public UserSessionProvider create(KeycloakSession session) {
        InfinispanConnectionProvider connections = session.getProvider(InfinispanConnectionProvider.class);
        Cache<String, SessionEntity> cache = connections.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);
        Cache<LoginFailureKey, LoginFailureEntity> loginFailures = connections.getCache(InfinispanConnectionProvider.LOGIN_FAILURE_CACHE_NAME);
        return new InfinispanUserSessionProvider(session, cache, loginFailures);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "infinispan";
    }

}

