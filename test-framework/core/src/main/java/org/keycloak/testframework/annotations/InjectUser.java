package org.keycloak.testframework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.DefaultUserConfig;
import org.keycloak.testframework.realm.UserConfig;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectUser {

    Class<? extends UserConfig> config() default DefaultUserConfig.class;

    /**
     * Controls the lifecycle of the resource
     */
    LifeCycle lifecycle() default LifeCycle.CLASS;

    /**
     * A ref must be set if a test requires multiple instances
     */
    String ref() default "";

    String realmRef() default "";
}
