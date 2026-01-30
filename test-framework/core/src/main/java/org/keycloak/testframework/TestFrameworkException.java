package org.keycloak.testframework;

import java.lang.annotation.Annotation;

import org.keycloak.testframework.injection.RequestedInstance;

public class TestFrameworkException extends RuntimeException {

    public TestFrameworkException(String message) {
        super(message);
    }

    public static TestFrameworkException typeMismatch(
            Class<? extends Annotation> annotation,
            Class<?> expectedType,
            Class<?> providedType) {
        return new TestFrameworkException(
                String.format("@%s requires %s (or its subclass) but field has type %s",
                        annotation.getSimpleName(),
                        expectedType.getSimpleName(),
                        providedType.getSimpleName())
        );
    }

    public static TestFrameworkException instanceAlreadyRequested(RequestedInstance<?, ?> requestedInstance, String fieldName) {
        String ref = requestedInstance.getRef() == null ? "" : requestedInstance.getRef();
        return new TestFrameworkException(
                String.format("A %s with ref=\"%s\" requested more than once. Second request was for field:\n\n@%s(ref=\"%s\")\n%s %s",
                        requestedInstance.getValueType().getSimpleName(),
                        ref,
                        requestedInstance.getAnnotation().annotationType().getSimpleName(),
                        ref,
                        requestedInstance.getValueType().getSimpleName(),
                        fieldName
                )

        );
    }

}
