package org.keycloak.models.cache;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MemoryCacheUserProviderFactory implements CacheUserProviderFactory {
    protected MemoryUserCache cache;

    @Override
    public CacheUserProvider create(KeycloakSession session) {
        return new DefaultCacheUserProvider(cache, session);
    }

    @Override
    public void init(Config.Scope config) {
        config.get("");
        cache = new MemoryUserCache();
        int size = config.getInt("maxSize", 10000);
        cache.setMaxUserCacheSize(size);
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
