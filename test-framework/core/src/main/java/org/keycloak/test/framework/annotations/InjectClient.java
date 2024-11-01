package org.keycloak.test.framework.annotations;

import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.realm.ClientConfig;
import org.keycloak.test.framework.realm.DefaultClientConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectClient {

    Class<? extends ClientConfig> config() default DefaultClientConfig.class;

    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    String realmRef() default "";
}
