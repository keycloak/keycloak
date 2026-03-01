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

import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.Validators;

/**
 * Validate that input value is {@link ValidatorConfig} and it is correct for validator (<code>inputHint</code> must be
 * ID of the validator config is for) by
 * {@link Validators#validateConfig(org.keycloak.models.KeycloakSession, String, ValidatorConfig)}. .
 */
public class ValidatorConfigValidator implements SimpleValidator {

    /**
     * Generic error messages for config validations - missing config value
     */
    public static final String MESSAGE_CONFIG_MISSING_VALUE = "error-validator-config-missing-value";
    /**
     * Generic error messages for config validations - invalid config value
     */
    public static final String MESSAGE_CONFIG_INVALID_VALUE = "error-validator-config-invalid-value";

    /**
     * Generic error messages for config validations - invalid config value - number expected
     */
    public static final String MESSAGE_CONFIG_INVALID_NUMBER_VALUE = "error-validator-config-invalid-number-value";

    /**
     * Generic error messages for config validations - invalid config value - boolean expected
     */
    public static final String MESSAGE_CONFIG_INVALID_BOOLEAN_VALUE = "error-validator-config-invalid-boolean-value";

    /**
     * Generic error messages for config validations - invalid config value - string expected
     */
    public static final String MESSAGE_CONFIG_INVALID_STRING_VALUE = "error-validator-config-invalid-string-value";

    public static final String ID = "validatorConfig";

    public static final ValidatorConfigValidator INSTANCE = new ValidatorConfigValidator();

    public ValidatorConfigValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input == null || input instanceof ValidatorConfig) {
            Validators.validateConfig(context.getSession(), inputHint, (ValidatorConfig) input).forEachError(context::addError);
        } else {
            context.addError(new ValidationError(ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE));
        }
        return context;
    }
}
