package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link com.sun.net.httpserver.HttpServer} instance that can be used to register or unregister additional
 * contexts to the Mock HTTP server used for tests. This should usually only be used by suppliers and not directly
 * by test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectHttpServer {

}
