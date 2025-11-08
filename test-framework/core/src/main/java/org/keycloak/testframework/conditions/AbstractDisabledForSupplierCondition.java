package org.keycloak.testframework.conditions;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import org.keycloak.testframework.injection.Extensions;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

abstract class AbstractDisabledForSupplierCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        Extensions extensions = Extensions.getInstance();

        Class<?> valueType = valueType();
        String valueTypeAlias = extensions.getValueTypeAlias().getAlias(valueType);

        Annotation annotation = getAnnotation(context, annotation());
        String[] excludedSuppliers = SupplierHelpers.getAnnotationField(annotation, "value");

        Supplier<?, ?> supplier = extensions.findSupplierByType(valueType);

        boolean excluded = Arrays.asList(excludedSuppliers).contains(supplier.getAlias());

        if (excluded) {
            return ConditionEvaluationResult.disabled("Disabled for " + valueTypeAlias + " " + supplier.getAlias());
        } else {
            return ConditionEvaluationResult.enabled("Enabled for " + valueTypeAlias + " " + supplier.getAlias());
        }
    }

    abstract Class<?> valueType();

    abstract Class<? extends Annotation> annotation();

    private <T extends Annotation> T getAnnotation(ExtensionContext context, Class<T> annotationClass) {
        T[] annotations = context.getElement().get().getAnnotationsByType(annotationClass);
        if (annotations.length == 0) {
            annotations = context.getParent().get().getElement().get().getAnnotationsByType(annotationClass);
        }
        return annotations[0];
    }
}
