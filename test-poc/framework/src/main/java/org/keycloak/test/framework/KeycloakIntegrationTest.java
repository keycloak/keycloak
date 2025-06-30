package org.keycloak.test.framework;

import org.keycloak.test.framework.server.DefaultKeycloakTestServerConfig;
import org.keycloak.test.framework.server.KeycloakTestServerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeycloakIntegrationTest {

    Class<? extends KeycloakTestServerConfig> config() default DefaultKeycloakTestServerConfig.class;

}
