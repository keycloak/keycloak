package org.keycloak.representations.admin.v2.validators;

import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.keycloak.representations.admin.v2.validation.ValidWebOrigin;

public class ValidWebOriginValidator implements ConstraintValidator<ValidWebOrigin, String> {

    private static final Pattern HOSTNAME_ORIGIN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://[a-zA-Z0-9.-]+(:[0-9]+)?$");
    private static final Pattern IPV6_ORIGIN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://\\[[0-9a-fA-F:]+](:[0-9]+)?$");

    @Override
    public boolean isValid(String origin, ConstraintValidatorContext context) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        if ("*".equals(origin) || "+".equals(origin)) {
            return true;
        }
        return HOSTNAME_ORIGIN.matcher(origin).matches() || IPV6_ORIGIN.matcher(origin).matches();
    }
}
