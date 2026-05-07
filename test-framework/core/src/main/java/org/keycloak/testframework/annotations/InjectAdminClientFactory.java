package org.keycloak.testframework.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;

/**
 * Injects a {@link org.keycloak.testframework.admin.AdminClientFactory} instance that can be used to create
 * {@link org.keycloak.admin.client.Keycloak} instances.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectAdminClientFactory {

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    /**
     * Controls the lifecycle of the resource
     */
    LifeCycle lifecycle() default LifeCycle.CLASS;
}
