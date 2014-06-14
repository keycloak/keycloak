package org.keycloak.models.cache;

import org.keycloak.Config;
import org.keycloak.provider.ProviderSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SimpleCacheKeycloakSessionFactory implements CacheKeycloakSessionFactory {
    protected KeycloakCache cache = new SimpleCache();

    @Override
    public CacheKeycloakSession create(ProviderSession providerSession) {
        return new DefaultCacheKeycloakSession(cache, providerSession);
    }

    @Override
    public void init(Config.Scope config) {
        config.get("");

    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getId() {
        return "simple";
    }
}
