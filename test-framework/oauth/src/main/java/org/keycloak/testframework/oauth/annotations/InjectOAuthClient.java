package org.keycloak.testframework.oauth.annotations;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.realm.ClientConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOAuthClient {

    Class<? extends ClientConfig> config() default DefaultOAuthClientConfiguration.class;

    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    String realmRef() default "";

    boolean kcAdmin() default false;

}
