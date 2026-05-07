package org.keycloak.testframework.oauth.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OIDCClientRepresentationBuilder;

/**
 *
 * @author rmartinc
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectCimdProvider {
    LifeCycle lifecycle() default LifeCycle.GLOBAL;

    Class<? extends OIDCClientRepresentationBuilder> config() default OIDCClientRepresentationBuilder.class;
}
