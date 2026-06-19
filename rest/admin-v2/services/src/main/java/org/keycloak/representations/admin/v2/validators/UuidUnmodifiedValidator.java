package org.keycloak.representations.admin.v2.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.validation.UuidUnmodified;

/**
 * @deprecated Use {@link ServerManagedFieldUnmodifiedValidator} instead.
 */
@Deprecated
public class UuidUnmodifiedValidator implements ConstraintValidator<UuidUnmodified, Object> {

    private String[] affectedFieldNames;

    @Override
    public void initialize(UuidUnmodified annotation) {
        this.affectedFieldNames = annotation.affectedFieldNames();
    }

    @Override
    public boolean isValid(Object representation, ConstraintValidatorContext context) {
        return ServerManagedFieldValidation.isValid(representation, affectedFieldNames, true, context);
    }
}
