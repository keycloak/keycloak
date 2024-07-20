package org.keycloak.test.framework.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public interface Supplier<T, S extends Annotation> {

    Class<S> getAnnotationClass();

    Class<T> getValueType();

    T getValue(InstanceContext<T, S> instanceContext);

    default LifeCycle getLifeCycle(S annotation) {
        if (annotation != null) {
            Optional<Method> lifecycle = Arrays.stream(annotation.annotationType().getMethods()).filter(m -> m.getName().equals("lifecycle")).findFirst();
            if (lifecycle.isPresent()) {
                try {
                    return (LifeCycle) lifecycle.get().invoke(annotation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return getDefaultLifecycle();
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
