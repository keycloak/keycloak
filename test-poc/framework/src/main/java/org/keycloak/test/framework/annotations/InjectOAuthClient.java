package org.keycloak.test.framework.annotations;

import org.keycloak.test.framework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.test.framework.realm.ClientConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOAuthClient {

    Class<? extends ClientConfig> config() default DefaultOAuthClientConfiguration.class;

}
