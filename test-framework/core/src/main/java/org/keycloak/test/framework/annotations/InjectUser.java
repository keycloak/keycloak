package org.keycloak.test.framework.annotations;

import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.DefaultUserConfig;
import org.keycloak.test.framework.realm.UserConfig;

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
