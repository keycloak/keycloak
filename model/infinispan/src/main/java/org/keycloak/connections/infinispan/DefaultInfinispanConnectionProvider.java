package org.keycloak.connections.infinispan;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProvider implements InfinispanConnectionProvider {

    private EmbeddedCacheManager cacheManager;

    public DefaultInfinispanConnectionProvider(EmbeddedCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        return cacheManager.getCache(name);
    }

    @Override
    public void close() {
    }

}
