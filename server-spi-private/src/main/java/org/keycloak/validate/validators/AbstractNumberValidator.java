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
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.AbstractSimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

/**
 * Abstract class for number validator. Supports min and max value validations using {@link #KEY_MIN} and
 * {@link #KEY_MAX} config options.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 */
public abstract class AbstractNumberValidator extends AbstractSimpleValidator implements ConfiguredProvider {

    public static final String MESSAGE_INVALID_NUMBER = "error-invalid-number";
    public static final String MESSAGE_NUMBER_OUT_OF_RANGE = "error-number-out-of-range";

    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";

    private final ValidatorConfig defaultConfig;
    
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(KEY_MIN);
        property.setLabel("Minimum");
        property.setHelpText("The minimal allowed value - this config is optional.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(KEY_MAX);
        property.setLabel("Maximum");
        property.setHelpText("The maximal allowed value - this config is optional.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    public AbstractNumberValidator() {
        // for reflection
        this(ValidatorConfig.EMPTY);
    }

    public AbstractNumberValidator(ValidatorConfig config) {
        this.defaultConfig = config;
    }
    
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    protected boolean skipValidation(Object value, ValidatorConfig config) {
        if (isIgnoreEmptyValuesConfigured(config) && (value == null || value instanceof String)) {
            return value == null || StringUtil.isBlank(value.toString());
        }
        return false;
    }

    @Override
    protected void doValidate(Object value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (config == null || config.isEmpty()) {
            config = defaultConfig;
        }

        Number number = null;

        if (value != null) {
            try {
                number = convert(value, config);
            } catch (NumberFormatException ignore) {
                // N/A
            }
        }

        if (number == null) {
            context.addError(new ValidationError(getId(), inputHint, MESSAGE_INVALID_NUMBER));
            return;
        }

        Number min = getMinMaxConfig(config, KEY_MIN);
        Number max = getMinMaxConfig(config, KEY_MAX);

        if (min != null && isFirstGreaterThanToSecond(min, number)) {
            context.addError(new ValidationError(getId(), inputHint, MESSAGE_NUMBER_OUT_OF_RANGE, min, max));
            return;
        }

        if (max != null && isFirstGreaterThanToSecond(number, max)) {
            context.addError(new ValidationError(getId(), inputHint, MESSAGE_NUMBER_OUT_OF_RANGE, min, max));
            return;
        }

        return;
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        Set<ValidationError> errors = new LinkedHashSet<>();

        if (config != null) {
            boolean containsMin = config.containsKey(KEY_MIN);
            boolean containsMax = config.containsKey(KEY_MAX);

            Number min = getMinMaxConfig(config, KEY_MIN);
            Number max = getMinMaxConfig(config, KEY_MAX);

            if (containsMin && min == null) {
                errors.add(new ValidationError(getId(), KEY_MIN, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, config.get(KEY_MIN)));
            }

            if (containsMax && max == null) {
                errors.add(new ValidationError(getId(), KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, config.get(KEY_MAX)));
            }

            if (errors.isEmpty() && containsMin && containsMax && (!isFirstGreaterThanToSecond(max, min))) {
                errors.add(new ValidationError(getId(), KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_VALUE));
            }
        }

        ValidationResult s = super.validateConfig(session, config);

        if (!s.isValid()) {
            errors.addAll(s.getErrors());
        }

        return new ValidationResult(errors);
    }

    /**
     * Convert input value to instance of Number supported by this validator.
     * 
     * @param value to convert
     * @param config
     * @return value converted to supported Number instance
     * @throws NumberFormatException if value is not convertible to supported Number instance so
     *             {@link #MESSAGE_INVALID_NUMBER} error is reported.
     */
    protected abstract Number convert(Object value, ValidatorConfig config);

    /**
     * Get config value for min and max validation bound as a Number supported by this validator
     * 
     * @param config to get from
     * @param key of the config value
     * @return bound value or null
     */
    protected abstract Number getMinMaxConfig(ValidatorConfig config, String key);

    /**
     * Compare two numbers of supported type (fed by {@link #convert(Object, ValidatorConfig)} and
     * {@link #getMinMaxConfig(ValidatorConfig, String)} )
     * 
     * @param n1
     * @param n2
     * @return true if first number is greater than second
     */
    protected abstract boolean isFirstGreaterThanToSecond(Number n1, Number n2);

}
