package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public interface Supplier<T, S extends Annotation> {

    Class<S> getAnnotationClass();

    Class<T> getValueType();

    T getValue(InstanceContext<T, S> instanceContext);

    default Object getAnnotationElementValue(S annotation, String annotationAttribute) {
        if (annotation != null) {
            Optional<Method> annotationMethod = Arrays.stream(annotation.annotationType().getMethods()).filter(m -> m.getName().equals(annotationAttribute)).findFirst();
            if (annotationMethod.isPresent()) {
                try {
                    return annotationMethod.get().invoke(annotation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return getAnnotationElementValue(annotationAttribute);
    }

    default Object getAnnotationElementValue(String annotationAttribute) {
        switch (annotationAttribute) {
            case SupplierHelpers.LIFECYCLE -> {
                return this.getDefaultLifecycle();
            }
            case SupplierHelpers.REF, SupplierHelpers.REALM_REF -> {
                return "";
            }
            default -> {
                return null;
            }
        }
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

}
