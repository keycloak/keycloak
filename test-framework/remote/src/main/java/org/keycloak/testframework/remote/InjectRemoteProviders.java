package org.keycloak.testframework.remote;

import org.keycloak.testframework.injection.LifeCycle;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface InjectRemoteProviders {

    LifeCycle lifecycle() default LifeCycle.GLOBAL;
}
