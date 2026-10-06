package org.keycloak.representations.admin.v2.validators;

import java.text.MessageFormat;

import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.validation.jakarta.ValidationContext;

final class ServerManagedFieldValidation {

    private ServerManagedFieldValidation() {
    }

    static boolean isValid(Object representation, String[] fields,
            ConstraintValidatorContext context) {
        if (representation == null) {
            return true;
        }

        PersistedFieldResolver fieldResolver = PersistedFieldResolvers.forType(representation.getClass());
        ValidationContext validationContext = ValidationContext.unwrap(context);
        Object persisted = fieldResolver.getPersisted(validationContext, representation);
        if (persisted == null) {
            return true;
        }
        boolean valid = true;
        for (String field : fields) {
            if (!isFieldValid(representation, persisted, field, context, fieldResolver)) {
                valid = false;
            }
        }
        return valid;
    }

    private static boolean isFieldValid(Object representation, Object persisted, String field,
            ConstraintValidatorContext context, PersistedFieldResolver fieldResolver) {
        Object providedValue = fieldResolver.getValue(representation, field);
        if (providedValue == null) {
            // note: this does not consider explicit nulls in a patch as being invalid for server managed fields
            // this is consistent with platforms like kubernetes
            return true;  
        }

        Object persistedValue = fieldResolver.getValue(persisted, field);

        if (persistedValue != null && persistedValue.equals(providedValue)) {
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
