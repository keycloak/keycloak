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

import java.util.Map;

public class LengthValidator implements CompactValidator {

    public static final LengthValidator INSTANCE = new LengthValidator();

    public static final String ID = "length";

    public static final String ERROR_INVALID_LENGTH = "error-invalid-length";

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
        int min = config.containsKey("min") ? Integer.parseInt(String.valueOf(config.get("min"))) : 0;
        int max = config.containsKey("max") ? Integer.parseInt(String.valueOf(config.get("max"))) : Integer.MAX_VALUE;

        int length = string.length();

        if (length < min) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, string));
        }

        if (length > max) {
            context.addError(new ValidationError(ID, inputHint, ERROR_INVALID_LENGTH, string));
        }

        return context;
    }
}
