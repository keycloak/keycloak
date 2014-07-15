package org.keycloak.models.cache;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MemoryCacheRealmProviderFactory implements CacheRealmProviderFactory {
    protected RealmCache cache = new MemoryRealmCache();

    @Override
    public CacheRealmProvider create(KeycloakSession session) {
        return new DefaultCacheRealmProvider(cache, session);
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
        return "mem";
    }
}
