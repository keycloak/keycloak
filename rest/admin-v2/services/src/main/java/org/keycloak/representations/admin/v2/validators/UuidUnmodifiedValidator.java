package org.keycloak.representations.admin.v2.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.representations.admin.v2.RepresentationWithUuid;
import org.keycloak.representations.admin.v2.validation.UuidUnmodified;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Validates that UUID provided by the client is not specified, or equal to the persisted UUID (in case of an update).
 * <p>
 * Additionally, it checks that the provided UUID does not exist in the system to prevent re-creation of a renamed resource.
 * This is useful for PUT create.
 * <p>
 * It assumes that the resource has a unique alias (e.g. name or clientId) that is used to identify the resource
 * in addition to the UUID.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class UuidUnmodifiedValidator implements ConstraintValidator<UuidUnmodified, RepresentationWithUuid> {

    @Override
    public boolean isValid(RepresentationWithUuid representation, ConstraintValidatorContext context) {
        Class<?> type = representation.getClass();
        UuidProvider uuidProvider = null;
        if (BaseClientRepresentation.class.isAssignableFrom(type)) {
            uuidProvider = new ClientUuidProvider();
        } else {
            throw new AssertionError("No UuidProvider defined for " + type);
        }
        
        String providedUuid = representation.getUuid();
        if (providedUuid == null || providedUuid.isEmpty()) { // no UUID provided, so nothing to validate
            return true;
        }

        ValidationContext validationContext = ValidationContext.unwrap(context);
        String persistedUuid = uuidProvider.getPersistedUuid(validationContext, representation);

        if (persistedUuid != null) { // resource exists
            if (persistedUuid.equals(providedUuid)) {
                return true;
            }
        } else if (!uuidProvider.uuidExists(validationContext, providedUuid)) { // additional check for PUT create to check the resource was just not renamed
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("uuid")
                .addConstraintViolation();
        return false;
    }
}
