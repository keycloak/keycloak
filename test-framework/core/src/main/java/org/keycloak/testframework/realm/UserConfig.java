package org.keycloak.testframework.realm;

/**
 * Declarative configuration for managed users
 */
public interface UserConfig {

    UserConfigBuilder configure(UserConfigBuilder user);

}
