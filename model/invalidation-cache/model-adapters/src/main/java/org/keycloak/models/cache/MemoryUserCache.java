package org.keycloak.models.cache;

import org.keycloak.models.cache.entities.CachedUser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class MemoryUserCache implements UserCache {

    protected int maxUserCacheSize = 10000;
    protected volatile boolean enabled = true;


    protected class RealmUsers {
        protected class LRUCache extends LinkedHashMap<String, CachedUser> {
            public LRUCache() {
                super(1000, 1.1F, true);
            }

            @Override
            public CachedUser put(String key, CachedUser value) {
                usersByUsername.put(value.getUsername(), value);
                if (value.getEmail() != null) {
                    usersByEmail.put(value.getEmail(), value);
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
                usersByUsername.remove(value.getUsername());
                if (value.getEmail() != null) usersByEmail.remove(value.getEmail());
            }
        }

        protected Map<String, CachedUser> usersById = Collections.synchronizedMap(new LRUCache());
        protected Map<String, CachedUser> usersByUsername = new ConcurrentHashMap<String, CachedUser>();
        protected Map<String, CachedUser> usersByEmail = new ConcurrentHashMap<String, CachedUser>();

    }

    protected ConcurrentHashMap<String, RealmUsers> realmUsers = new ConcurrentHashMap<String, RealmUsers>();

    public int getMaxUserCacheSize() {
        return maxUserCacheSize;
    }

    public void setMaxUserCacheSize(int maxUserCacheSize) {
        this.maxUserCacheSize = maxUserCacheSize;
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
    public CachedUser getCachedUser(String realmId, String id) {
        if (realmId == null || id == null) return null;
        RealmUsers users = realmUsers.get(realmId);
        if (users == null) return null;
        return users.usersById.get(id);
    }

    @Override
    public void invalidateCachedUser(String realmId, CachedUser user) {
        RealmUsers users = realmUsers.get(realmId);
        if (users == null) return;
        users.usersById.remove(user.getId());
    }

    @Override
    public void invalidateCachedUserById(String realmId, String id) {
        RealmUsers users = realmUsers.get(realmId);
        if (users == null) return;
        users.usersById.remove(id);
    }

    @Override
    public void addCachedUser(String realmId, CachedUser user) {
        RealmUsers users = realmUsers.get(realmId);
        if (users == null) {
            users = new RealmUsers();
            realmUsers.put(realmId, users);
        }
        users.usersById.put(user.getId(), user);
    }

    @Override
    public CachedUser getCachedUserByUsername(String realmId, String name) {
        if (realmId == null || name == null) return null;
        RealmUsers users = realmUsers.get(realmId);
        if (users == null) return null;
        CachedUser user = users.usersByUsername.get(name);
        if (user == null) return null;
        users.usersById.get(user.getId()); // refresh cache entry age
        return user;
    }

    @Override
    public CachedUser getCachedUserByEmail(String realmId, String email) {
        if (realmId == null || email == null) return null;
        RealmUsers users = realmUsers.get(realmId);
        if (users == null) return null;
        CachedUser user = users.usersByEmail.get(email);
        if (user == null) return null;
        users.usersById.get(user.getId()); // refresh cache entry age
        return user;
    }

    @Override
    public void invalidateRealmUsers(String realmId) {
        realmUsers.remove(realmId);
    }

    @Override
    public void clear() {
        realmUsers.clear();
    }
}
