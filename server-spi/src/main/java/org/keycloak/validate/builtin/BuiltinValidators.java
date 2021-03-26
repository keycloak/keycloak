package org.keycloak.validate.builtin;

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A registry of builtin {@link Validator} implementations.
 */
public class BuiltinValidators {

    private static final Map<String, Validator> INTERNAL_VALIDATORS;

    static {
        List<CompactValidator> list = Arrays.asList(
                LengthValidator.INSTANCE,
                NotEmptyValidator.INSTANCE
        );

        Map<String, Validator> validators = new HashMap<>();

        for (CompactValidator validator : list) {
            validators.put(validator.getId(), validator);
        }

        INTERNAL_VALIDATORS = validators;
    }

    public static Validator getValidatorById(String id) {
        return INTERNAL_VALIDATORS.get(id);
    }

    public static Map<String, Validator> getValidators() {
        return Collections.unmodifiableMap(INTERNAL_VALIDATORS);
    }

    public static Validator notEmpty() {
        return NotEmptyValidator.INSTANCE;
    }

    public static Validator length() {
        return LengthValidator.INSTANCE;
    }
}