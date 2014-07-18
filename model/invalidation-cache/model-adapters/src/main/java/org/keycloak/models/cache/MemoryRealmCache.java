package org.keycloak.models.cache;

import org.keycloak.models.cache.entities.CachedApplication;
import org.keycloak.models.cache.entities.CachedOAuthClient;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MemoryRealmCache implements RealmCache {

    protected ConcurrentHashMap<String, CachedRealm> realmCache = new ConcurrentHashMap<String, CachedRealm>();
    protected ConcurrentHashMap<String, CachedRealm> realmCacheByName = new ConcurrentHashMap<String, CachedRealm>();
    protected ConcurrentHashMap<String, CachedApplication> applicationCache = new ConcurrentHashMap<String, CachedApplication>();
    protected ConcurrentHashMap<String, CachedOAuthClient> clientCache = new ConcurrentHashMap<String, CachedOAuthClient>();
    protected ConcurrentHashMap<String, CachedRole> roleCache = new ConcurrentHashMap<String, CachedRole>();
    protected volatile boolean enabled = true;

    @Override
    public void clear() {
        realmCache.clear();
        realmCacheByName.clear();
        applicationCache.clear();
        clientCache.clear();
        roleCache.clear();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        clear();
        this.enabled = enabled;
        clear();
    }

    @Override
    public CachedRealm getCachedRealm(String id) {
        if (!enabled) return null;
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
        if (!enabled) return;
        realmCache.put(realm.getId(), realm);
        realmCacheByName.put(realm.getName(), realm);

    }

    @Override
    public CachedRealm getCachedRealmByName(String name) {
        if (!enabled) return null;
        return realmCacheByName.get(name);
    }

    @Override
    public CachedApplication getApplication(String id) {
        if (!enabled) return null;
        return applicationCache.get(id);
    }

    @Override
    public void invalidateApplication(CachedApplication app) {
        applicationCache.remove(app.getId());
    }

    @Override
    public void addCachedApplication(CachedApplication app) {
        if (!enabled) return;
        applicationCache.put(app.getId(), app);
    }

    @Override
    public void invalidateCachedApplicationById(String id) {
        applicationCache.remove(id);
    }

    @Override
    public CachedOAuthClient getOAuthClient(String id) {
        if (!enabled) return null;
        return clientCache.get(id);
    }

    @Override
    public void invalidateOAuthClient(CachedOAuthClient client) {
        clientCache.remove(client.getId());
    }

    @Override
    public void addCachedOAuthClient(CachedOAuthClient client) {
        if (!enabled) return;
        clientCache.put(client.getId(), client);
    }

    @Override
    public void invalidateCachedOAuthClientById(String id) {
        clientCache.remove(id);
    }

    @Override
    public CachedRole getRole(String id) {
        if (!enabled) return null;
        return roleCache.get(id);
    }

    @Override
    public void invalidateRole(CachedRole role) {
        roleCache.remove(role);
    }

    @Override
    public void invalidateRoleById(String id) {
        roleCache.remove(id);
    }

    @Override
    public void addCachedRole(CachedRole role) {
        if (!enabled) return;
        roleCache.put(role.getId(), role);
    }

    @Override
    public void invalidateCachedRoleById(String id) {
        roleCache.remove(id);
    }
}
