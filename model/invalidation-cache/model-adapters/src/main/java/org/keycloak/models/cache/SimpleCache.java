package org.keycloak.models.cache;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.provider.ProviderSession;
import org.keycloak.provider.ProviderSessionFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SimpleCache implements KeycloakCache {

    protected ConcurrentHashMap<String, CachedRealm> realmCache = new ConcurrentHashMap<String, CachedRealm>();
    protected ConcurrentHashMap<String, CachedRealm> realmCacheByName = new ConcurrentHashMap<String, CachedRealm>();

    @Override
    public void clear() {
        realmCache.clear();
        realmCacheByName.clear();
    }

    @Override
    public CachedRealm getCachedRealm(String id) {
        return realmCache.get(id);
    }

    @Override
    public void invalidateCachedRealm(CachedRealm realm) {
        realmCache.remove(realm.getId());
        realmCacheByName.remove(realm.getName());
    }

    @Override
    public void invalidateCachedRealmById(String id) {
        CachedRealm cached = realmCache.remove(id);
        if (cached != null) realmCacheByName.remove(cached.getName());
    }


    @Override
    public void addCachedRealm(CachedRealm realm) {
        realmCache.put(realm.getId(), realm);
        realmCache.put(realm.getName(), realm);

    }

    @Override
    public CachedRealm getCachedRealmByName(String name) {
        return realmCacheByName.get(name);
    }
}
