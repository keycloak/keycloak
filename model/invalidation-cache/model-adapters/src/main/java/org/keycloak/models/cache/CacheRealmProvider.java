package org.keycloak.models.cache;

import org.keycloak.models.RealmProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CacheRealmProvider extends RealmProvider {
    RealmProvider getDelegate();

    boolean isEnabled();
    void setEnabled(boolean enabled);

    void registerRealmInvalidation(String id);

    void registerApplicationInvalidation(String id);

    void registerRoleInvalidation(String id);

    void registerOAuthClientInvalidation(String id);

    void registerUserInvalidation(String id);
}
