package org.keycloak.testframework.realm;

/**
 * Declarative configuration for managed clients
 */
public interface ClientConfig {

    ClientBuilder configure(ClientBuilder client);

}
