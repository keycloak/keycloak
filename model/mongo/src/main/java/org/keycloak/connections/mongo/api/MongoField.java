package org.keycloak.connections.mongo.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Target({METHOD, FIELD})
@Documented
@Retention(RUNTIME)
public @interface MongoField {

    // TODO: fieldName add lazy loading?
}
