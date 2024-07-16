package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;

public class RequestedInstance<T, A extends Annotation> {

    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Class<? extends T> valueType;

    public RequestedInstance(Supplier<T, A> supplier, A annotation, Class<? extends T> valueType) {
        this.supplier = supplier;
        this.annotation = annotation;
        this.valueType = valueType;
    }

    public Supplier<T, A> getSupplier() {
        return supplier;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Class<? extends T> getValueType() {
        return valueType;
    }
}
