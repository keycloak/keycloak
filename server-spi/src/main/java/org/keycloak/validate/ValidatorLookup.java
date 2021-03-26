/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
