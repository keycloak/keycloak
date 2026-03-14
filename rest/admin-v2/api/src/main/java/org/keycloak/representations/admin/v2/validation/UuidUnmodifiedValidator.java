package org.keycloak.representations.admin.v2.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.BaseRepresentation;
import org.keycloak.validation.jakarta.ValidationContext;

/**
 * Validates that UUID provided by the client is not specified, or equal to the persisted UUID (in case of an update).
 * It assumes that the resource has a unique alias (e.g. name or clientId) that is used to identify the resource,
 * and that the persisted alias can be retrieved using the provided UUID.
 *
 * @author Vaclav Muzikar <vmuzikar@ibm.com>
 */
public class UuidUnmodifiedValidator implements ConstraintValidator<UuidUnmodified, BaseRepresentation> {

    private AliasProvider aliasProvider;

    @Override
    public void initialize(UuidUnmodified constraintAnnotation) {
        try {
            aliasProvider = constraintAnnotation.aliasProvider().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate UUID provider: " + constraintAnnotation.aliasProvider().getName(), e);
        }
    }

    @Override
    public boolean isValid(BaseRepresentation representation, ConstraintValidatorContext context) {
        String providedUuid = representation.getUuid();
        String providedAlias = aliasProvider.getAliasFromRepresentation(representation);
        if (providedUuid == null || providedAlias == null) {
            return true;
        }

        ValidationContext validationContext = ValidationContext.unwrap(context);

        String persistedAlias = aliasProvider.getPersistedAlias(validationContext.session(), validationContext.realm(), providedUuid);

        if (persistedAlias != null && persistedAlias.equals(providedAlias)) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("uuid")
                .addConstraintViolation();
        return false;
    }
}
