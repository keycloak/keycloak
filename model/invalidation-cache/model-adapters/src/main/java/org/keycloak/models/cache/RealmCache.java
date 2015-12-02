package org.keycloak.models.cache;

import org.keycloak.models.cache.entities.CachedClient;
import org.keycloak.models.cache.entities.CachedGroup;
import org.keycloak.models.cache.entities.CachedRealm;
import org.keycloak.models.cache.entities.CachedRole;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmCache {
    void clear();

    CachedRealm getCachedRealm(String id);

    void invalidateCachedRealm(CachedRealm realm);

    void addCachedRealm(CachedRealm realm);

    CachedRealm getCachedRealmByName(String name);

    void invalidateCachedRealmById(String id);

    CachedClient getApplication(String id);

    void invalidateApplication(CachedClient app);

    void evictCachedApplicationById(String id);

    void addCachedClient(CachedClient app);

    void invalidateCachedApplicationById(String id);

    CachedRole getRole(String id);

    void invalidateRole(CachedRole role);

    void addCachedRole(CachedRole role);

    void invalidateCachedRoleById(String id);

    void invalidateRoleById(String id);

    CachedGroup getGroup(String id);

    void invalidateGroup(CachedGroup role);

    void addCachedGroup(CachedGroup role);

    void invalidateCachedGroupById(String id);

    void invalidateGroupById(String id);

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
