package org.keycloak.models.realms;

import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.Provider;

import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface RealmProvider extends Provider {

    Realm createRealm(String name);
    Realm createRealm(String id, String name);

    Realm getRealm(String id);
    Realm getRealmByName(String name);
    List<Realm> getRealms();
    boolean removeRealm(String id);

    Role getRoleById(String id, String realm);
    Application getApplicationById(String id, String realm);
    OAuthClient getOAuthClientById(String id, String realm);

    KeycloakTransaction getTransaction();

    void close();

}
