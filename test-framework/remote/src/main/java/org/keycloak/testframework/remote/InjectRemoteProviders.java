package org.keycloak.testframework.remote;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.keycloak.testframework.injection.LifeCycle;

@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRemoteProviders {

    LifeCycle lifecycle() default LifeCycle.GLOBAL;
}
