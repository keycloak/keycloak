package org.keycloak.representations.admin.v2.validators;

import java.text.MessageFormat;

import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.validation.jakarta.ValidationContext;

final class ServerManagedFieldValidation {

    private ServerManagedFieldValidation() {
    }

    static boolean isValid(Object representation, String[] fields, boolean rejectExistingValueOnCreate,
            ConstraintValidatorContext context) {
        if (representation == null) {
            return true;
        }

        boolean valid = true;
        for (String field : fields) {
            if (!isFieldValid(representation, field, rejectExistingValueOnCreate, context)) {
                valid = false;
            }
        }
        return valid;
    }

    private static boolean isFieldValid(Object representation, String field, boolean rejectExistingValueOnCreate,
            ConstraintValidatorContext context) {
        PersistedFieldResolver fieldResolver = PersistedFieldResolvers.forType(representation.getClass());

        String providedValue = fieldResolver.getProvidedValue(representation, field);
        if (providedValue == null || providedValue.isEmpty()) {
            return true;
        }

        ValidationContext validationContext = ValidationContext.unwrap(context);
        String persistedValue = fieldResolver.getPersistedValue(validationContext, representation, field);

        if (persistedValue != null) {
            if (persistedValue.equals(providedValue)) {
                return true;
            }
        } else if (!rejectExistingValueOnCreate
                || !fieldResolver.valueExists(validationContext, field, providedValue)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        String template = context.getDefaultConstraintMessageTemplate();
        context.buildConstraintViolationWithTemplate(MessageFormat.format(template, field))
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
