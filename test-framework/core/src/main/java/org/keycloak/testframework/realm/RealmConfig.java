package org.keycloak.testframework.realm;

/**
 * Declarative configuration for managed realms
 */
public interface RealmConfig {

    RealmConfigBuilder configure(RealmConfigBuilder realm);

}
