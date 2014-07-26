package org.keycloak.models;

import org.keycloak.provider.Provider;

import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface RealmProvider extends Provider {
    // Note: The reason there are so many query methods here is for layering a cache on top of an persistent KeycloakSession

    RealmModel createRealm(String name);
    RealmModel createRealm(String id, String name);
    RealmModel getRealm(String id);
    RealmModel getRealmByName(String name);

    RoleModel getRoleById(String id, RealmModel realm);
    ApplicationModel getApplicationById(String id, RealmModel realm);
    OAuthClientModel getOAuthClientById(String id, RealmModel realm);
    List<RealmModel> getRealms();
    boolean removeRealm(String id);

    void close();
}
