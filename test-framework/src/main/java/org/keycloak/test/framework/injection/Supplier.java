package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;

public interface Supplier<T, S extends Annotation> {

    Class<S> getAnnotationClass();

    Class<T> getValueType();

    T getValue(InstanceContext<T, S> instanceContext);

    default String getRef(S annotation) {
        return StringUtil.convertEmptyToNull(SupplierHelpers.getAnnotationField(annotation, AnnotationFields.REF));
    }

    default LifeCycle getLifeCycle(S annotation) {
        return SupplierHelpers.getAnnotationField(annotation, AnnotationFields.LIFECYCLE, getDefaultLifecycle());
    }

    default LifeCycle getDefaultLifecycle() {
        return LifeCycle.CLASS;
    }

    boolean compatible(InstanceContext<T, S> a, RequestedInstance<T, S> b);

    default void close(InstanceContext<T, S> instanceContext) {
    }

    default String getAlias() {
        return getClass().getSimpleName();
    }

    default void onBeforeEach(InstanceContext<T, S> instanceContext) {
    }

}
