package org.keycloak.representations.admin.v2.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.validation.ProtocolUnmodified;

/**
 * @deprecated Use {@link ServerManagedFieldUnmodifiedValidator} instead.
 */
@Deprecated
public class ProtocolUnmodifiedValidator implements ConstraintValidator<ProtocolUnmodified, Object> {

    private String[] affectedFieldNames;

    @Override
    public void initialize(ProtocolUnmodified annotation) {
        this.affectedFieldNames = annotation.affectedFieldNames();
    }

    @Override
    public boolean isValid(Object representation, ConstraintValidatorContext context) {
        return ServerManagedFieldValidation.isValid(representation, affectedFieldNames, false, context);
    }
}
