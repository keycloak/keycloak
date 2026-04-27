package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.KeycloakIntegrationTestExtension;
import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Enables the test framework for tests
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith({KeycloakIntegrationTestExtension.class})
public @interface KeycloakIntegrationTest {

    /**
     * Used to define custom configuration for the Keycloak server
     */
    Class<? extends KeycloakServerConfig> config() default DefaultKeycloakServerConfig.class;

}
