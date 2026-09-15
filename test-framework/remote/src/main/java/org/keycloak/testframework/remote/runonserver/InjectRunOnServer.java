package org.keycloak.testframework.remote.runonserver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.testframework.injection.LifeCycle;

/**
 * Injects a {@link RunOnServerClient} to execute code within the Keycloak server. Classes are serialized and sent
 * to the Keycloak server when needed
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InjectRunOnServer {

    LifeCycle lifecycle() default LifeCycle.CLASS;

    String ref() default "";

    String realmRef() default "";

    String[] permittedPackages() default "";

}
