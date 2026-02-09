/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.userprofile.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.utils.StringUtil;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.validators.ValidatorConfigValidator;

import static org.keycloak.validate.validators.ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE;
import static org.keycloak.validate.validators.ValidatorConfigValidator.MESSAGE_CONFIG_MISSING_VALUE;

public class MultiValueValidator implements SimpleValidator, ConfiguredProvider {

    public static final String ID = "multivalued";
    public static final String MESSAGE_INVALID_SIZE = "error-invalid-multivalued-size";
    public static final String KEY_MIN = "min";
    public static final String KEY_MAX = "max";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object value, String inputHint, ValidationContext context, ValidatorConfig config) {
        AttributeContext attributeContext = UserProfileAttributeValidationContext.from(context).getAttributeContext();

        Long min = Optional.ofNullable(config.getLong(KEY_MIN)).orElse(getDefaultMinSize(context));
        Long max = config.getLong(KEY_MAX);

        if (!(value instanceof Collection)) {
            addError(inputHint, context, min, max);
            return context;
        }

        long length = ((Collection<String>) value).stream().filter(StringUtil::isNotBlank).count();

        if (length == 0 && attributeContext.getMetadata().isRequired(attributeContext)) {
            // if no value is set and attribute is required, do not validate in favor of the required validator
            return context;
        }

        if (!(length >= min && length <= max)) {
            addError(inputHint, context, min, max);
            return context;
        }

        return context;
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        if (ValidatorConfig.isEmpty(config)) {
            return ValidationResult.of(new ValidationError(ID, KEY_MAX, MESSAGE_CONFIG_MISSING_VALUE));
        }

        Set<ValidationError> errors = new HashSet<>();
        Long min = config.getLong(KEY_MIN);
        Long max = config.getLong(KEY_MAX);

        if (min == null && config.containsKey(KEY_MIN)) {
            errors.add(new ValidationError(ID, KEY_MIN, MESSAGE_CONFIG_INVALID_NUMBER_VALUE));
        }

        if (max == null) {
            errors.add(new ValidationError(ID, KEY_MAX,
                    config.containsKey(KEY_MAX) ? MESSAGE_CONFIG_INVALID_NUMBER_VALUE : MESSAGE_CONFIG_MISSING_VALUE));
        } else if (min != null && (min > max)) {
            errors.add(new ValidationError(ID, KEY_MAX, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_VALUE));
        }

        return ValidationResult.of(errors);
    }

    @Override
    public String getHelpText() {
        return "Multivalued validator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> properties = new ArrayList<>();
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(KEY_MIN);
        property.setLabel("Minimum size");
        property.setHelpText("The minimum size");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        properties.add(property);
        property = new ProviderConfigProperty();
        property.setName(KEY_MAX);
        property.setLabel("Maximum size");
        property.setHelpText("The maximum size");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        properties.add(property);
        return properties;
    }

    private long getDefaultMinSize(ValidationContext context) {
        AttributeContext attributeContext = UserProfileAttributeValidationContext.from(context).getAttributeContext();
        AttributeMetadata metadata = attributeContext.getMetadata();
        return metadata.isRequired(attributeContext) ? 1 : 0;
    }

    private void addError(String inputHint, ValidationContext context, Long min, Long max) {
        context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_SIZE, min, max));
    }
}
