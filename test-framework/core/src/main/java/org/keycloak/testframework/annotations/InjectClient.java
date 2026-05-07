package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.DefaultClientConfig;

/**
 * Injects a {@link org.keycloak.testframework.realm.ManagedClient} used to create a client within the realm
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectClient {

    /**
     * Used to define a custom configuration for the client
     */
    Class<? extends ClientConfig> config() default DefaultClientConfig.class;

    /**
     * Controls the lifecycle of the resource
     */
    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    /**
     * Set to attach to the non-default realm
     */
    String realmRef() default "";

    /**
     * Attach to an existing client instead of creating one; when attaching to an existing client the config will be ignored
     * and the client will not be deleted automatically.
     *
     * @return the client-id of the existing client to attach to
     */
    String attachTo() default "";

}
