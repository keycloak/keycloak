package org.keycloak.testframework.realm;

/**
 * Declarative configuration for managed users
 */
public interface UserConfig {

    UserBuilder configure(UserBuilder user);

}
