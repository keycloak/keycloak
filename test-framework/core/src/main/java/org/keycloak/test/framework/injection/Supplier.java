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

    /**
     * Allows suppliers to decorate values injected by other suppliers
     *
     * @param object the object to decorate
     * @param instanceContext the deployed instance for this supplier; or <code>null</code> if the value has not been created yet
     */
    default void decorate(Object object, InstanceContext<T, S> instanceContext) {
    }

}
