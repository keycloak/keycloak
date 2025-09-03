package org.keycloak.testframework.oauth.annotations;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.DefaultOAuthIdentityProviderConfig;
import org.keycloak.testframework.oauth.OAuthIdentityProviderConfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectOAuthIdentityProvider {

    LifeCycle lifecycle() default LifeCycle.GLOBAL;

    Class<? extends OAuthIdentityProviderConfig> config() default DefaultOAuthIdentityProviderConfig.class;

}
