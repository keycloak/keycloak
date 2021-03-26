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

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class LengthValidator implements CompactValidator {

    public static final LengthValidator INSTANCE = new LengthValidator();

    public static final String ID = "length";

    public static final String ERROR_INVALID_LENGTH = "error-invalid-length";
    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";

    private LengthValidator() {
        // prevent instantiation
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, Map<String, Object> config) {

        if (input == null) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, input));
            return context;
        }

        if (!(input instanceof String)) {
            return context;
        }

        // TODO make config value extraction more robust

        String string = (String) input;
        int min = config.containsKey(KEY_MIN) ? Integer.parseInt(String.valueOf(config.get(KEY_MIN))) : 0;
        int max = config.containsKey(KEY_MAX) ? Integer.parseInt(String.valueOf(config.get(KEY_MAX))) : Integer.MAX_VALUE;

        int length = string.length();

        if (length < min) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, string));
        }

        if (length > max) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, string));
        }

        return context;
    }

    @Override
    public ValidationResult validateConfig(Map<String, Object> config) {

        if (config == null) {
            // new don't require configuration
            return ValidationResult.OK;
        }


        boolean containsMin = config.containsKey(KEY_MIN);
        boolean containsMax = config.containsKey(KEY_MAX);
        if (!containsMin && containsMax) {
            return ValidationResult.OK;
        }

        Object maybeMin = config.get(KEY_MIN);
        Object maybeMax = config.get(KEY_MAX);

        Set<ValidationError> errors = new LinkedHashSet<>();
        if (containsMin && !(maybeMin instanceof Integer)) {
            errors.add(new ValidationError(ID, KEY_MIN, ERROR_INVALID_VALUE, maybeMin));
        }

        if (containsMax && !(maybeMax instanceof Integer)) {
            errors.add(new ValidationError(ID, KEY_MAX, ERROR_INVALID_VALUE, maybeMax));
        }

        return new ValidationResult(errors);
    }
}
