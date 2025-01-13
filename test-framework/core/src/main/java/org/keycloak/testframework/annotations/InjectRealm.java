package org.keycloak.testframework.annotations;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.DefaultRealmConfig;
import org.keycloak.testframework.realm.RealmConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectRealm {

    Class<? extends RealmConfig> config() default DefaultRealmConfig.class;

    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    boolean createRealm() default true;

}
