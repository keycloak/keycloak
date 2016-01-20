package org.keycloak.models.cache;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CacheUserProvider extends UserProvider {
    void clear();
    UserProvider getDelegate();
    void registerUserInvalidation(RealmModel realm, String id);
}
