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
package org.keycloak.validate.builtin;

import org.keycloak.models.KeycloakSession;
import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class LengthValidator implements CompactValidator {

    public static final LengthValidator INSTANCE = new LengthValidator();

    public static final String ID = "length";

    public static final String MESSAGE_INVALID_LENGTH = "error-invalid-length";
    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";

    private static final ValidatorConfig DEFAULT_CONFIG;

    static {
        Map<String, Object> config = new HashMap<>();
        config.put(KEY_MIN, 0);
        config.put(KEY_MAX, 255);

        DEFAULT_CONFIG = ValidatorConfig.configFromMap(config);
    }

    private LengthValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input == null) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_LENGTH, input));
            return context;
        }

        if (!(input instanceof String)) {
            context.addError(new ValidationError(ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE, input));
            return context;
        }

        String string = (String) input;
        int min = config.getIntOrDefault(KEY_MIN, 0);
        int max = config.getIntOrDefault(KEY_MAX, Integer.MAX_VALUE);

        int length = string.length();

        if (length < min) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_LENGTH, input));
        }

        if (length > max) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_LENGTH, input));
        }

        return context;
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {

        if (config == null || config == ValidatorConfig.EMPTY) {
            // new don't require configuration
            return ValidationResult.OK;
        }

        boolean containsMin = config.containsKey(KEY_MIN);
        boolean containsMax = config.containsKey(KEY_MAX);
        if (!(containsMin || containsMax)) {
            return ValidationResult.OK;
        }

        Object maybeMin = config.get(KEY_MIN);
        Object maybeMax = config.get(KEY_MAX);

        Set<ValidationError> errors = new LinkedHashSet<>();
        if (containsMin && !(maybeMin instanceof Integer)) {
            errors.add(new ValidationError(ID, KEY_MIN, ValidationError.MESSAGE_INVALID_VALUE, maybeMin));
        }

        if (containsMax && !(maybeMax instanceof Integer)) {
            errors.add(new ValidationError(ID, KEY_MAX, ValidationError.MESSAGE_INVALID_VALUE, maybeMax));
        }

        return new ValidationResult(errors);
    }

    @Override
    public ValidatorConfig getDefaultConfig(KeycloakSession session) {
        return DEFAULT_CONFIG;
    }
}
