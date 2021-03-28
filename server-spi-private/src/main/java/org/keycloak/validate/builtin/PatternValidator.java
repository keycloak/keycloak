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
import org.keycloak.validate.ValidatorConfig;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternValidator implements CompactValidator {

    public static final String ID = "pattern";

    public static final PatternValidator INSTANCE = new PatternValidator();

    public static final String KEY_PATTERN = "pattern";

    public static final String MESSAGE_NO_MATCH = "error-no-match";

    private PatternValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (!(input instanceof String)) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_VALUE, input));
            return context;
        }

        String string = (String) input;

        Pattern pattern = config.getPattern(KEY_PATTERN);
        if (!pattern.matcher(string).matches()) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_NO_MATCH, input));
        }

        return context;
    }

    @Override
    public ValidationResult validateConfig(ValidatorConfig config) {

        Set<ValidationError> errors = new LinkedHashSet<>();
        Object maybePattern = config.get(KEY_PATTERN);
        try {
            Pattern pattern = config.getPattern(KEY_PATTERN);
            if (pattern == null) {
                errors.add(new ValidationError(ID, KEY_PATTERN, MESSAGE_INVALID_VALUE, maybePattern));
            }
        } catch (PatternSyntaxException pse) {
            errors.add(new ValidationError(ID, KEY_PATTERN, MESSAGE_INVALID_VALUE, maybePattern));
        }

        return new ValidationResult(errors);
    }
}
