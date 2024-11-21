package org.keycloak.test.framework.realm;

import org.keycloak.test.framework.injection.InstanceContext;

import java.lang.annotation.Annotation;

public interface RealmConfigInterceptor<T, S extends Annotation> {

    RealmConfigBuilder intercept(RealmConfigBuilder realm, InstanceContext<T, S> instanceContext);

}
