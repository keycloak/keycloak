package org.keycloak.testframework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.http.simple.SimpleHttp} that can be used to do HTTP requests within tests.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectSimpleHttp {
}
