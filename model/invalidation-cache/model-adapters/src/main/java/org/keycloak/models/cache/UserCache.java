package org.keycloak.models.cache;

import org.keycloak.models.cache.entities.CachedUser;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserCache {
    void clear();

    CachedUser getCachedUser(String realmId, String id);

    void invalidateCachedUser(String realmId, CachedUser user);

    void addCachedUser(String realmId, CachedUser user);

    CachedUser getCachedUserByUsername(String realmId, String name);

    CachedUser getCachedUserByEmail(String realmId, String name);

    void invalidateCachedUserById(String realmId, String id);

    void invalidateRealmUsers(String realmId);

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
