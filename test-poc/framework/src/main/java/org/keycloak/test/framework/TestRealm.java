package org.keycloak.test.framework;

import org.keycloak.test.framework.realm.DefaultRealmConfig;
import org.keycloak.test.framework.realm.RealmConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TestRealm {

    Class<? extends RealmConfig> config() default DefaultRealmConfig.class;

}
