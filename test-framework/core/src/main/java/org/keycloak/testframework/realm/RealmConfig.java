package org.keycloak.testframework.realm;

/**
 * Declarative configuration for managed realms
 */
public interface RealmConfig {

    RealmBuilder configure(RealmBuilder realm);

}
