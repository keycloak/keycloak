package org.keycloak.models.utils.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A criteria that matches a property based on its annotations
 *
 * @see PropertyCriteria
 */
public class AnnotatedPropertyCriteria implements PropertyCriteria {
    private final Class<? extends Annotation> annotationClass;

    public AnnotatedPropertyCriteria(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }

    @Override
    public boolean methodMatches(Method m) {
        return m.isAnnotationPresent(annotationClass);
    }

}
