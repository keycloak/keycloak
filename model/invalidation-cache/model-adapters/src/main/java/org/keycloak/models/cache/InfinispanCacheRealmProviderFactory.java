package org.keycloak.models.cache;

import org.infinispan.Cache;
import org.keycloak.Config;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanCacheRealmProviderFactory implements CacheRealmProviderFactory {

    protected final ConcurrentHashMap<String, String> realmLookup = new ConcurrentHashMap<String, String>();

    @Override
    public CacheRealmProvider create(KeycloakSession session) {
        Cache<String, Object> cache = session.getProvider(InfinispanConnectionProvider.class).getCache("realms");
        RealmCache realmCache = new InfinispanRealmCache(cache, realmLookup);
        return new InfinispanCacheRealmProvider(realmCache, session);
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
