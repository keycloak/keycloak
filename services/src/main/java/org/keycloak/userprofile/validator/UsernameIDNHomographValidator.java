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
package org.keycloak.userprofile.validator;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.validation.Validation;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check that User Profile username is provided. Expects List of Strings as input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class UsernameIDNHomographValidator implements SimpleValidator, ConfiguredProvider {

    public static final String ID = "up-username-not-idn-homograph";

    public static final String CFG_ERROR_MESSAGE = "error-message";

    public static final String MESSAGE_NO_MATCH = "error-username-invalid-character";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(CFG_ERROR_MESSAGE);
        property.setLabel("Error message key");
        property.setHelpText("Key of the error message in i18n bundle. Default message key is " + MESSAGE_NO_MATCH);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        String value = null;

        if (values != null && !values.isEmpty()) {
            value = values.get(0);
        }

        if (!Validation.isBlank(value) && !Validation.isUsernameValid(value)) {
            context.addError(new ValidationError(ID, inputHint, getErrorMessageKey(inputHint, config)));
        }

        return context;
    }

    protected String getErrorMessageKey(String inputHint, ValidatorConfig config) {
        String cfg = config.getString(CFG_ERROR_MESSAGE);
        return (cfg != null && !cfg.isBlank()) ? cfg : MESSAGE_NO_MATCH;
    }

    @Override
    public String getHelpText() {
        return "The field can contain only latin characters and common unicode characters. Useful for the fields, which can be subject of IDN homograph attacks (typically username).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
