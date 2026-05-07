package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.DefaultRealmConfig;
import org.keycloak.testframework.realm.RealmConfig;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectRealm {

    /**
     * Used to define a custom configuration for the realm
     */
    Class<? extends RealmConfig> config() default DefaultRealmConfig.class;

    /**
     * Loads custom configuration from a json file on the classpath
     */
    String fromJson() default "";

    /**
     * Controls the lifecycle of the resource
     */
    LifeCycle lifecycle() default LifeCycle.CLASS;

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    /**
     * Attach to an existing realm instead of creating one; when attaching to an existing realm the config will be ignored
     * and the realm will not be deleted automatically.
     *
     * @return the name of the existing realm to attach to
     */
    String attachTo() default "";

}
