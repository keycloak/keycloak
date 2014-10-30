package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheRealmProviderFactory;
import org.keycloak.models.cache.DefaultCacheRealmProvider;
import org.keycloak.models.cache.RealmCache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheRealmProviderFactory implements CacheRealmProviderFactory {

    protected final ConcurrentHashMap<String, String> realmLookup = new ConcurrentHashMap<String, String>();

    @Override
    public CacheRealmProvider create(KeycloakSession session) {
        Cache<String, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.REALM_CACHE_NAME);
        RealmCache realmCache = new InfinispanRealmCache(cache, realmLookup);
        return new DefaultCacheRealmProvider(realmCache, session);
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
