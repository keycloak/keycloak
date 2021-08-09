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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

/**
 * String value length validation - accepts plain string and collection of strings, for basic behavior like null/blank
 * values handling and collections support see {@link AbstractStringValidator}. Validator trims String value before the
 * length validation, can be disabled by {@link #KEY_TRIM_DISABLED} boolean configuration entry set to
 * <code>true</code>.
 * <p>
 * Configuration have to be always provided, with at least one of {@link #KEY_MIN} and {@link #KEY_MAX}.
 */
public class LengthValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final LengthValidator INSTANCE = new LengthValidator();

    public static final String ID = "length";

    public static final String MESSAGE_INVALID_LENGTH = "error-invalid-length";
    public static final String MESSAGE_INVALID_LENGTH_TOO_SHORT = "error-invalid-length-too-short";
    public static final String MESSAGE_INVALID_LENGTH_TOO_LONG = "error-invalid-length-too-long";

    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";
    public static final String KEY_TRIM_DISABLED = "trim-disabled";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(KEY_MIN);
        property.setLabel("Minimum length");
        property.setHelpText("The minimum length");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(KEY_MAX);
        property.setLabel("Maximum length");
        property.setHelpText("The maximum length");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(KEY_TRIM_DISABLED);
        property.setLabel("Trimming disabled");
        property.setHelpText("Disable trimming of the String value before the length check");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        Integer min = config.getInt(KEY_MIN);
        Integer max = config.getInt(KEY_MAX);

        if (!config.getBooleanOrDefault(KEY_TRIM_DISABLED, Boolean.FALSE)) {
            value = value.trim();
        }

        int length = value.length();

        if (config.containsKey(KEY_MIN) && length < min.intValue()) {
            context.addError(new ValidationError(ID, inputHint, selectErrorMessage(config), min, max));
            return;
        }

        if (config.containsKey(KEY_MAX) && length > max.intValue()) {
            context.addError(new ValidationError(ID, inputHint, selectErrorMessage(config), min, max));
            return;
        }

    }
    
    /**
     * Select error message depending on the allowed length interval bound configuration.
     */
    protected String selectErrorMessage(ValidatorConfig config) {
        if (!config.containsKey(KEY_MAX)) {
            return MESSAGE_INVALID_LENGTH_TOO_SHORT;
        } else if (!config.containsKey(KEY_MIN)) {
            return MESSAGE_INVALID_LENGTH_TOO_LONG;
        } else {
            return MESSAGE_INVALID_LENGTH;
        }
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {

        Set<ValidationError> errors = new LinkedHashSet<>();
        if (config == null || config == ValidatorConfig.EMPTY) {
            errors.add(new ValidationError(ID, KEY_MIN, ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE));
            errors.add(new ValidationError(ID, KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE));
        } else {

            if (config.containsKey(KEY_TRIM_DISABLED) && (config.getBoolean(KEY_TRIM_DISABLED) == null)) {
                errors.add(new ValidationError(ID, KEY_TRIM_DISABLED, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_BOOLEAN_VALUE, config.get(KEY_TRIM_DISABLED)));
            }

            boolean containsMin = config.containsKey(KEY_MIN);
            boolean containsMax = config.containsKey(KEY_MAX);

            if (!(containsMin || containsMax)) {
                errors.add(new ValidationError(ID, KEY_MIN, ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE));
                errors.add(new ValidationError(ID, KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE));
            } else {

                if (containsMin && config.getInt(KEY_MIN) == null) {
                    errors.add(new ValidationError(ID, KEY_MIN, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, config.get(KEY_MIN)));
                }

                if (containsMax && config.getInt(KEY_MAX) == null) {
                    errors.add(new ValidationError(ID, KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, config.get(KEY_MAX)));
                }

                if (errors.isEmpty() && containsMin && containsMax && (config.getInt(KEY_MIN) > config.getInt(KEY_MAX))) {
                    errors.add(new ValidationError(ID, KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_VALUE));
                }
            }
        }
        return new ValidationResult(errors);
    }

    @Override
    public String getHelpText() {
        return "Length validator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
