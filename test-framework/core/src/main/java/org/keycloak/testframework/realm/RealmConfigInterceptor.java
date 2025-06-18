package org.keycloak.testframework.realm;

import org.keycloak.testframework.injection.InstanceContext;

import java.lang.annotation.Annotation;

public interface RealmConfigInterceptor<T, S extends Annotation> {

    RealmConfigBuilder intercept(RealmConfigBuilder realm, InstanceContext<T, S> instanceContext);

}
