package org.keycloak.connections.mongo.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Target({TYPE})
@Documented
@Retention(RUNTIME)
@Inherited
public @interface MongoCollection {

    String collectionName();
}
