package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;

public interface Supplier<T, S extends Annotation> {

    Class<S> getAnnotationClass();

    Class<T> getValueType();

    InstanceWrapper<T, S> getValue(Registry registry, S annotation);

    default InstanceWrapper<T, S> getValue(Registry registry, S annotation, Class<? extends T> valueType) {
        return getValue(registry, annotation);
    }

    boolean compatible(InstanceWrapper<T, S> a, RequestedInstance<T, S> b);

    default void close(T value) {
    }

    default void close(InstanceWrapper<T, S> instanceWrapper) {
        close(instanceWrapper.getValue());
    }

    default String getAlias() {
        return getClass().getSimpleName();
    }

}
