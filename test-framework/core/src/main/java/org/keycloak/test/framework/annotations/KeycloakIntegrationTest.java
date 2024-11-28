package org.keycloak.test.framework.annotations;

import org.keycloak.test.framework.server.DefaultKeycloakServerConfig;
import org.keycloak.test.framework.server.KeycloakServerConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeycloakIntegrationTest {

    Class<? extends KeycloakServerConfig> config() default DefaultKeycloakServerConfig.class;

}
