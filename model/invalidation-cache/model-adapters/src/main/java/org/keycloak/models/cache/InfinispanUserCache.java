package org.keycloak.models.cache;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserCache implements UserCache {

    protected static final Logger logger = Logger.getLogger(InfinispanRealmCache.class);

    protected volatile boolean enabled = true;

    protected final Cache<String, CachedUser> cache;

    protected final ConcurrentHashMap<String, String> usernameLookup;

    protected final ConcurrentHashMap<String, String> emailLookup;

    public InfinispanUserCache(Cache<String, CachedUser> cache, ConcurrentHashMap<String, String> usernameLookup, ConcurrentHashMap<String, String> emailLookup) {
        this.cache = cache;
        this.usernameLookup = usernameLookup;
        this.emailLookup = emailLookup;
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
        CachedUser user = cache.get(id);
        return user != null && realmId.equals(user.getRealm()) ? user : null;
    }

    @Override
    public void invalidateCachedUser(String realmId, CachedUser user) {
        logger.tracev("Invalidating user {0}", user.getId());
        cache.remove(user.getId());
    }

    @Override
    public void invalidateCachedUserById(String realmId, String id) {
        logger.tracev("Invalidating user {0}", id);
        cache.remove(id);
    }

    @Override
    public void addCachedUser(String realmId, CachedUser user) {
        logger.tracev("Adding user {0}", user.getId());
        cache.put(user.getId(), user);
    }

    @Override
    public CachedUser getCachedUserByUsername(String realmId, String name) {
        String id = usernameLookup.get(name);
        return id != null ? getCachedUser(realmId, id) : null;
    }

    @Override
    public CachedUser getCachedUserByEmail(String realmId, String email) {
        String id = emailLookup.get(email);
        return id != null ? getCachedUser(realmId, id) : null;
    }

    @Override
    public void invalidateRealmUsers(String realmId) {
        logger.tracev("Invalidating users for realm {0}", realmId);
        for (Map.Entry<String, CachedUser> u : cache.entrySet()) {
            if (u.getValue().getRealm().equals(realmId)) {
                cache.remove(u.getKey());
            }
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }

}
