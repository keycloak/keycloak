package org.keycloak.validate;

import org.keycloak.models.KeycloakSession;
import org.keycloak.validate.builtin.BuiltinValidators;

/**
 * Allows to search for {@link Validator} implementations by id.
 */
public class ValidatorLookup {

    // TODO this should be part of KeycloakSession API later

    /**
     * Look-up up for a built-in or registered validator with the given validatorName.
     *
     * @param id the id of the validator.
     */
    public static Validator validator(KeycloakSession session, String id) {

        // Fast-path for internal Validators
        Validator validator = BuiltinValidators.getValidatorById(id);
        if (validator != null) {
            return validator;
        }

        // Lookup validator in registry
        return session.getProvider(Validator.class, id);
    }
}
