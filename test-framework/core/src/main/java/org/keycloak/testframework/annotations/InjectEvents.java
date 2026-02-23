package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.events.Events} instance that can be used to poll login events from Keycloak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectEvents {

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    /**
     * Set to attach to the non-default realm
     */
    String realmRef() default "";

}
