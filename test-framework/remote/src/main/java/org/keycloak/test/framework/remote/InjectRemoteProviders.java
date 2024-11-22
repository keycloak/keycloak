package org.keycloak.test.framework.remote;

import org.keycloak.test.framework.injection.LifeCycle;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRemoteProviders {

    LifeCycle lifecycle() default LifeCycle.GLOBAL;
}
