package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.apache.http.client.HttpClient} that can be used to do HTTP requests within tests. See
 * {@link InjectSimpleHttp} as an alternative that provides a simpler API.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectHttpClient {

    boolean followRedirects() default true;

}
