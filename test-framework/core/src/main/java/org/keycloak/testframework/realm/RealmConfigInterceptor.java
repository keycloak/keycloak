package org.keycloak.testframework.realm;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.injection.InstanceContext;

public interface RealmConfigInterceptor<T, S extends Annotation> {

    RealmBuilder intercept(RealmBuilder realm, InstanceContext<T, S> instanceContext);

    /**
     * Controls which realms this interceptor applies to. By default, an interceptor applies to every realm created
     * within the test class. Override to only intercept a specific realm, for example the realm referenced by the
     * <code>realmRef</code> of the interceptor's own annotation.
     *
     * @param annotation the annotation of the interceptor itself
     * @param realmInstanceContext the instance context of the realm being created
     * @return <code>true</code> if the realm should be intercepted
     */
    default boolean appliesTo(S annotation, InstanceContext<?, ?> realmInstanceContext) {
        return true;
    }

}
