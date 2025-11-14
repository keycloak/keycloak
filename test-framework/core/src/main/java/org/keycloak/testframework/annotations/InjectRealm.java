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

    Class<? extends RealmConfig> config() default DefaultRealmConfig.class;

    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    /**
     * Attach to an existing realm instead of creating one; when attaching to an existing realm the config will be ignored
     * and the realm will not be deleted automatically.
     *
     * @return the name of the existing realm to attach to
     */
    String attachTo() default "";

}
