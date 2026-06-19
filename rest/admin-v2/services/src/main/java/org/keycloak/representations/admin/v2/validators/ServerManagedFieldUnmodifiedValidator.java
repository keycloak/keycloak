package org.keycloak.representations.admin.v2.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.validation.ServerManagedFieldUnmodified;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Validates that a server-managed field provided by the client is not specified, or equal to the persisted value
 * (in case of an update).
 * <p>
 * When {@link ServerManagedFieldUnmodified#rejectExistingValueOnCreate()} is enabled, it additionally checks that
 * the provided value does not already exist in the system to prevent re-creation of a renamed resource.
 * This is useful for PUT create.
 * <p>
 * It assumes that the resource has a unique alias (e.g. name or clientId) that is used to identify the resource
 * in addition to the server-managed field.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class ServerManagedFieldUnmodifiedValidator implements ConstraintValidator<ServerManagedFieldUnmodified, Object> {

    private String field;
    private boolean rejectExistingValueOnCreate;

    @Override
    public void initialize(ServerManagedFieldUnmodified annotation) {
        this.field = annotation.field();
        this.rejectExistingValueOnCreate = annotation.rejectExistingValueOnCreate();
    }

    @Override
    public boolean isValid(Object representation, ConstraintValidatorContext context) {
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
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(field)
                .addConstraintViolation();
        return false;
    }
}
