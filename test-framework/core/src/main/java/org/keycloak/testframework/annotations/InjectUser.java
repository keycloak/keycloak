package org.keycloak.testframework.annotations;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.DefaultUserConfig;
import org.keycloak.testframework.realm.UserConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectUser {

    Class<? extends UserConfig> config() default DefaultUserConfig.class;

    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    String realmRef() default "";
}
