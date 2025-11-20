package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.server.DefaultKeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeycloakIntegrationTest {

    Class<? extends KeycloakServerConfig> config() default DefaultKeycloakServerConfig.class;

}
