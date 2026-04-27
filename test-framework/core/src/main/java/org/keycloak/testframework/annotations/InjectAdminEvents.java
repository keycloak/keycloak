package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects a {@link org.keycloak.testframework.events.AdminEvents} instance that can be used to poll admin events from Keycloak
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectAdminEvents {

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    /**
     * Set to attach to the non-default realm
     */
    String realmRef() default "";

}
