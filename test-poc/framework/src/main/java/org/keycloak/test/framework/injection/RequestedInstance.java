package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;

public class RequestedInstance<T, A extends Annotation> {

    private final Supplier<T, A> supplier;
    private final A annotation;
    private final Class<? extends T> valueType;
    private final LifeCycle lifeCycle;
    private final String ref;

    public RequestedInstance(Supplier<T, A> supplier, A annotation, Class<? extends T> valueType) {
        this.supplier = supplier;
        this.annotation = annotation;
        this.valueType = valueType;
        this.lifeCycle = (LifeCycle) supplier.getAnnotationElementValue(annotation, SupplierHelpers.LIFECYCLE);
        this.ref = (String) supplier.getAnnotationElementValue(annotation, SupplierHelpers.REF);
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

    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public String getRef() {
        return ref;
    }

}
