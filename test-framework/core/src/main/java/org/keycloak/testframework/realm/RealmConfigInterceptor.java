package org.keycloak.testframework.realm;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.injection.InstanceContext;

public interface RealmConfigInterceptor<T, S extends Annotation> {

    RealmBuilder intercept(RealmBuilder realm, InstanceContext<T, S> instanceContext);

}
