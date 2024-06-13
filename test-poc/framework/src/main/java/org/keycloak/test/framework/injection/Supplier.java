package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;

public interface Supplier<T, S extends Annotation> {

    Class<S> getAnnotationClass();

    Class<T> getValueType();

    InstanceWrapper<T, S> getValue(Registry registry, S annotation);

    LifeCycle getLifeCycle();

    boolean compatible(InstanceWrapper<T, S> a, InstanceWrapper<T, S> b);

    default void close(T instance) {}

}
