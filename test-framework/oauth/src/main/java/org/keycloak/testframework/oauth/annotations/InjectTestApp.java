package org.keycloak.testframework.oauth.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.oauth.TestApp} that can be used to mock an OAuth client
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectTestApp {

}
