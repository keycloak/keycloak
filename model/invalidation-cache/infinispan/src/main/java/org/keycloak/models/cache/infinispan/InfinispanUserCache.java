package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.cache.entities.CachedUser;

import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class InfinispanUserCache implements UserCache {

    protected static final Logger logger = Logger.getLogger(InfinispanRealmCache.class);

    protected volatile boolean enabled = true;

    protected final Cache<String, CachedUser> cache;

    protected final InfinispanCacheUserProviderFactory.RealmLookup usernameLookup;

    protected final InfinispanCacheUserProviderFactory.RealmLookup emailLookup;

    public InfinispanUserCache(Cache<String, CachedUser> cache, InfinispanCacheUserProviderFactory.RealmLookup usernameLookup, InfinispanCacheUserProviderFactory.RealmLookup emailLookup) {
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
        String id = usernameLookup.get(realmId, name);
        return id != null ? getCachedUser(realmId, id) : null;
    }

    @Override
    public CachedUser getCachedUserByEmail(String realmId, String email) {
        String id = emailLookup.get(realmId, email);
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
