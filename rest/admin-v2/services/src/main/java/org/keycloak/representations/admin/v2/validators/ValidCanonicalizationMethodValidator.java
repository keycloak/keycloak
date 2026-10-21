package org.keycloak.representations.admin.v2.validators;

import java.util.Set;
import javax.xml.crypto.dsig.CanonicalizationMethod;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.validation.ValidCanonicalizationMethod;

public class ValidCanonicalizationMethodValidator implements ConstraintValidator<ValidCanonicalizationMethod, String> {

    private static final Set<String> VALID = Set.of(
            CanonicalizationMethod.EXCLUSIVE,
            CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
            CanonicalizationMethod.INCLUSIVE,
            CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
            CanonicalizationMethod.INCLUSIVE_11,
            CanonicalizationMethod.INCLUSIVE_11_WITH_COMMENTS
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return VALID.contains(value);
    }
}
