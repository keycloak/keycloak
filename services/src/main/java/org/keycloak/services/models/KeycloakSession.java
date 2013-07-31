package org.keycloak.services.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface KeycloakSession {
    KeycloakTransaction getTransaction();

    RealmModel createRealm(String name);
    RealmModel createRealm(String id, String name);
    RealmModel getRealm(String id);
    void deleteRealm(RealmModel realm);

    void close();
}
