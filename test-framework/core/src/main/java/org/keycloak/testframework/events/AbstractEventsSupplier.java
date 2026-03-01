package org.keycloak.testframework.events;

import java.lang.annotation.Annotation;
import java.util.List;

import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfigInterceptor;

@SuppressWarnings("rawtypes")
public abstract class AbstractEventsSupplier<E extends AbstractEvents, A extends Annotation> implements Supplier<E, A>, RealmConfigInterceptor<E, A> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<E, A> instanceContext) {
        return DependenciesBuilder.create(ManagedRealm.class, SupplierHelpers.getAnnotationField(instanceContext.getAnnotation(), "realmRef")).build();
    }

    @Override
    public E getValue(InstanceContext<E, A> instanceContext) {
        String realmRef = SupplierHelpers.getAnnotationField(instanceContext.getAnnotation(), "realmRef");
        ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, realmRef);
        return createValue(realm);
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<E, A> a, RequestedInstance<E, A> b) {
        return true;
    }

    @Override
    public void onBeforeEach(InstanceContext<E, A> instanceContext) {
        instanceContext.getValue().testStarted();
    }

    @Override
    public void close(InstanceContext<E, A> instanceContext) {
        instanceContext.getValue().clear();
    }

    protected abstract E createValue(ManagedRealm realm);

}
