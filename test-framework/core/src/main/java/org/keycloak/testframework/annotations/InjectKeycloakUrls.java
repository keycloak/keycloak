package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.server.KeycloakUrls} instance that can be used to discover various
 * endpoints offered by the Keycloak server
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectKeycloakUrls {

}
