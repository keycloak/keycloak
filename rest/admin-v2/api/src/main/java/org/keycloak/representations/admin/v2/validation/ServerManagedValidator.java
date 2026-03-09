package org.keycloak.representations.admin.v2.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link ServerManaged} constraint.
 * Validates that server-managed fields are not populated by the user.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class ServerManagedValidator implements ConstraintValidator<ServerManaged, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.trim().isEmpty();
    }
}
