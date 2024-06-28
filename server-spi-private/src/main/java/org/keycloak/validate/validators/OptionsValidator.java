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
 * Validation against list of allowed values - accepts plain string and collection of strings (every value is validated against allowed values), for basic behavior like null/blank
 * values handling and collections support see {@link AbstractStringValidator}.
 * <p>
 * Configuration have to be always provided using {@link #KEY_OPTIONS} option, which have to contain <code>List</code> of <code>String</code> values.
 */
public class OptionsValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final OptionsValidator INSTANCE = new OptionsValidator();

    public static final String ID = "options";

    public static final String KEY_OPTIONS = "options";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(KEY_OPTIONS);
        property.setLabel("Options");
        property.setHelpText("List of allowed options");
        property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {

        List<String> allowedValues = config.getStringListOrDefault(KEY_OPTIONS);
        if (allowedValues == null || !allowedValues.contains(value)) {
            context.addError(new ValidationError(ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE));
        }
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {

        Set<ValidationError> errors = new LinkedHashSet<>();
        if (config == null || !config.containsKey(KEY_OPTIONS)) {
            errors.add(new ValidationError(ID, KEY_OPTIONS, ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE));
        } else if (!(config.get(KEY_OPTIONS) instanceof List)) {
            errors.add(new ValidationError(ID, KEY_OPTIONS, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_VALUE, "must be list of values"));
        }
        return new ValidationResult(errors);
    }

    @Override
    public String getHelpText() {
        return "Options validator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
