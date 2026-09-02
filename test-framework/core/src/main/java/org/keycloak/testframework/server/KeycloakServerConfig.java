package org.keycloak.testframework.server;

/**
 * Declarative configuration for the managed Keycloak server
 */
public interface KeycloakServerConfig {

    KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config);

}
