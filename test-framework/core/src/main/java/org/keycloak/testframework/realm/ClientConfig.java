package org.keycloak.testframework.realm;

/**
 * Declarative configuration for managed clients
 */
public interface ClientConfig {

    ClientConfigBuilder configure(ClientConfigBuilder client);

}
