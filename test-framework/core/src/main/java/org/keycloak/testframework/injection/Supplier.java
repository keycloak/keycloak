package org.keycloak.testframework.injection;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

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

    default int order() {
        return SupplierOrder.DEFAULT;
    }

    default Set<Class<?>> dependencies() {
        return Collections.emptySet();
    }

}
