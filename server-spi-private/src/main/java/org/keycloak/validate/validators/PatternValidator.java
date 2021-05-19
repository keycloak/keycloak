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
package org.keycloak.validate.validators;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.keycloak.models.KeycloakSession;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validate String against configured RegEx pattern - accepts plain string and collection of strings, for basic behavior
 * like null/blank values handling and collections support see {@link AbstractStringValidator}.
 */
public class PatternValidator extends AbstractStringValidator {

    public static final String ID = "pattern";

    public static final PatternValidator INSTANCE = new PatternValidator();

    public static final String KEY_PATTERN = "pattern";

    public static final String MESSAGE_NO_MATCH = "error-pattern-no-match";

    private PatternValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        Pattern pattern = config.getPattern(KEY_PATTERN);

        if (!pattern.matcher(value).matches()) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_NO_MATCH, value, config.getString(KEY_PATTERN)));
        }
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        Set<ValidationError> errors = new LinkedHashSet<>();

        if (config == null || config == ValidatorConfig.EMPTY || !config.containsKey(KEY_PATTERN)) {
            errors.add(new ValidationError(ID, KEY_PATTERN, ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE));
        } else {
            Object maybePattern = config.get(KEY_PATTERN);
            try {
                Pattern pattern = config.getPattern(KEY_PATTERN);
                if (pattern == null) {
                    errors.add(new ValidationError(ID, KEY_PATTERN, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_VALUE, maybePattern));
                }
            } catch (PatternSyntaxException pse) {
                errors.add(new ValidationError(ID, KEY_PATTERN, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_VALUE, maybePattern));
            }
        }
        return new ValidationResult(errors);
    }

}
