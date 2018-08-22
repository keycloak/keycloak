package org.keycloak.models.cache.infinispan.authorization;

import org.infinispan.Cache;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.List;

public class PermissionCacheManager {
    private final Cache<String, CacheEntry> cache;

    public PermissionCacheManager(KeycloakSession session) {
        this.cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.AUTHORIZATION_PERMISSION_CACHE_NAME);
    }

    private CacheEntry getCacheEntry(String tokenID) {
        return cache.get(tokenID);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public List<Permission> get(String tokenID) {
        CacheEntry result = getCacheEntry(tokenID);

        if (result != null) {
            return result.result();
        }

        return null;
    }

    public boolean containsKey(String tokenID) {
        return cache.containsKey(tokenID);
    }

    public void put(String tokenID, List<Permission> result) {
        cache.put(tokenID, new CacheEntry(tokenID, result));
    }

    public void clear() {
        cache.clear();
    }

    private static final class CacheEntry {
        private String tokenID;
        private List<Permission> permissions;

        public CacheEntry(String tokenID, List<Permission> permissions) {
            this.permissions = permissions;
            this.tokenID = tokenID;
        }

        public String key() {
            return tokenID;
        }

        public List<Permission> result() {
            return permissions;
        }

    }
}
