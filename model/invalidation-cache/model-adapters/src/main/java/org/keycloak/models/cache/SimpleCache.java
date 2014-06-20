package org.keycloak.models.cache;

import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.entities.CachedApplication;
import org.keycloak.models.cache.entities.CachedOAuthClient;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SimpleCache implements KeycloakCache {

    protected ConcurrentHashMap<String, CachedRealm> realmCache = new ConcurrentHashMap<String, CachedRealm>();
    protected ConcurrentHashMap<String, CachedRealm> realmCacheByName = new ConcurrentHashMap<String, CachedRealm>();
    protected ConcurrentHashMap<String, CachedApplication> applicationCache = new ConcurrentHashMap<String, CachedApplication>();
    protected ConcurrentHashMap<String, CachedOAuthClient> clientCache = new ConcurrentHashMap<String, CachedOAuthClient>();
    protected ConcurrentHashMap<String, CachedRole> roleCache = new ConcurrentHashMap<String, CachedRole>();

    protected int maxUserCacheSize = 10000;
    protected boolean userCacheEnabled = true;

    protected Map<String, CachedUser> usersById = Collections.synchronizedMap(new LRUCache());
    protected Map<String, CachedUser> usersByUsername = new ConcurrentHashMap<String, CachedUser>();
    protected Map<String, CachedUser> usersByEmail = new ConcurrentHashMap<String, CachedUser>();

    protected class LRUCache extends LinkedHashMap<String, CachedUser> {
        public LRUCache() {
            super(1000, 1.1F, true);
        }

        @Override
        public CachedUser put(String key, CachedUser value) {
            usersByUsername.put(value.getUsernameKey(), value);
            if (value.getEmail() != null) {
                usersByEmail.put(value.getEmailKey(), value);
            }
            return super.put(key, value);
        }

        @Override
        public CachedUser remove(Object key) {
            CachedUser user = super.remove(key);
            if (user == null) return null;
            removeUser(user);
            return user;
        }

        @Override
        public void clear() {
            super.clear();
            usersByUsername.clear();
            usersByEmail.clear();
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedUser> eldest) {
            boolean evict = size() > maxUserCacheSize;
            if (evict) {
                removeUser(eldest.getValue());
            }
            return evict;
        }

        private void removeUser(CachedUser value) {
            usersByUsername.remove(value.getUsernameKey());
            if (value.getEmail() != null) usersByEmail.remove(value.getEmailKey());
        }
    }

    public int getMaxUserCacheSize() {
        return maxUserCacheSize;
    }

    public void setMaxUserCacheSize(int maxUserCacheSize) {
        this.maxUserCacheSize = maxUserCacheSize;
    }

    public boolean isUserCacheEnabled() {
        return userCacheEnabled;
    }

    public void setUserCacheEnabled(boolean userCacheEnabled) {
        this.userCacheEnabled = userCacheEnabled;
    }

    @Override
    public CachedUser getCachedUser(String id) {
        if (!userCacheEnabled) return null;
        return usersById.get(id);
    }

    @Override
    public void invalidateCachedUser(CachedUser user) {
        if (!userCacheEnabled) return;
        usersById.remove(user.getId());
    }

    @Override
    public void invalidateCachedUserById(String id) {
        if (!userCacheEnabled) return;
        usersById.remove(id);
    }

    @Override
    public void addCachedUser(CachedUser user) {
        if (!userCacheEnabled) return;
        usersById.put(user.getId(), user);
    }

    @Override
    public CachedUser getCachedUserByUsername(String name, RealmModel realm) {
        if (!userCacheEnabled) return null;
        CachedUser user = usersByUsername.get(realm.getId() + "." +name);
        if (user == null) return null;
        usersById.get(user.getId()); // refresh cache entry age
        return user;
    }

    @Override
    public CachedUser getCachedUserByEmail(String name, RealmModel realm) {
        if (!userCacheEnabled) return null;
        CachedUser user = usersByEmail.get(realm.getId() + "." +name);
        if (user == null) return null;
        usersById.get(user.getId()); // refresh cache entry age
        return user;
    }

    @Override
    public void invalidedCachedUserById(String id) {
        if (!userCacheEnabled) return;
        usersById.remove(id);
    }

    @Override
    public void clear() {
        realmCache.clear();
        realmCacheByName.clear();
        applicationCache.clear();
        clientCache.clear();
        roleCache.clear();
        usersById.clear();
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
        realmCacheByName.put(realm.getName(), realm);

    }

    @Override
    public CachedRealm getCachedRealmByName(String name) {
        return realmCacheByName.get(name);
    }

    @Override
    public CachedApplication getApplication(String id) {
        return applicationCache.get(id);
    }

    @Override
    public void invalidateApplication(CachedApplication app) {
        applicationCache.remove(app.getId());
    }

    @Override
    public void addCachedApplication(CachedApplication app) {
        applicationCache.put(app.getId(), app);
    }

    @Override
    public void invalidateCachedApplicationById(String id) {
        applicationCache.remove(id);
    }

    @Override
    public CachedOAuthClient getOAuthClient(String id) {
        return clientCache.get(id);
    }

    @Override
    public void invalidateOAuthClient(CachedOAuthClient client) {
        clientCache.remove(client.getId());
    }

    @Override
    public void addCachedOAuthClient(CachedOAuthClient client) {
        clientCache.put(client.getId(), client);
    }

    @Override
    public void invalidateCachedOAuthClientById(String id) {
        clientCache.remove(id);
    }

    @Override
    public CachedRole getRole(String id) {
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
        roleCache.put(role.getId(), role);
    }

    @Override
    public void invalidateCachedRoleById(String id) {
        roleCache.remove(id);
    }
}
