package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;

public interface Supplier<T, S extends Annotation> {

    Class<S> getAnnotationClass();

    Class<T> getValueType();

    default InstanceWrapper<T, S> getValue(Registry registry) {
        return getValue(registry, (S) null);
    }

    InstanceWrapper<T, S> getValue(Registry registry, S annotation);

    default InstanceWrapper<T, S> getValue(Registry registry, InstanceWrapper<T, S> wrapper) {
        return getValue(registry, wrapper.getAnnotation());
    }

    LifeCycle getLifeCycle();

    boolean compatible(InstanceWrapper<T, S> a, InstanceWrapper<T, S> b);

    default void close(T instance) {}

}
