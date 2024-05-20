package org.keycloak.test.framework;

import org.keycloak.test.framework.server.smallrye_config.TestConfigSource;
import org.keycloak.test.framework.server.smallrye_config.DefaultTestConfigSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KeycloakIntegrationTest {

    Class<? extends TestConfigSource> config() default DefaultTestConfigSource.class;

}
