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
import java.util.List;
import java.util.Set;

import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.utils.EmailValidationUtil;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

/**
 * Email format validation - accepts plain string and collection of strings, for basic behavior like null/blank values
 * handling and collections support see {@link AbstractStringValidator}.
 */
public class EmailValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final String ID = "email";

    public static final EmailValidator INSTANCE = new EmailValidator();

    public static final String MESSAGE_INVALID_EMAIL = "error-invalid-email";

    public static final String MESSAGE_NON_ASCII_LOCAL_PART_EMAIL = "error-non-ascii-local-part-email";

    public static final String MAX_LOCAL_PART_LENGTH_PROPERTY = "max-local-length";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        Integer maxEmailLocalPartLength = null;
        if (config != null) {
            maxEmailLocalPartLength = config.getInt(MAX_LOCAL_PART_LENGTH_PROPERTY);
        }

        if (!(maxEmailLocalPartLength != null
                ? EmailValidationUtil.isValidEmail(value, maxEmailLocalPartLength)
                : EmailValidationUtil.isValidEmail(value))) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_EMAIL, value));
            return;
        }

        final KeycloakSession session = context.getSession();
        if (session == null) {
            return;
        }

        final RealmModel realm = session.getContext().getRealm();
        if (realm == null || realm.getSmtpConfig() == null || realm.getSmtpConfig().isEmpty()
                || "true".equals(realm.getSmtpConfig().get(EmailSenderProvider.CONFIG_ALLOW_UTF8))) {
            // UTF-8 non-ascii chars allowed because no smtp configuration or allowutf8 is enabled
            return;
        }

        final int idx = value.lastIndexOf('@');
        if (idx < 0) {
            return;
        }

        final String localPart = value.substring(0, idx);
        if (!localPart.chars().allMatch(c -> c < 128)) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_NON_ASCII_LOCAL_PART_EMAIL));
        }
    }
    
    @Override
    public String getHelpText() {
        return "Email format validator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create().property()
                .name(MAX_LOCAL_PART_LENGTH_PROPERTY)
                .type(ProviderConfigProperty.STRING_TYPE)
                .label("Maximum length for the local part")
                .helpText("Maximum length for the local part of the email")
                .defaultValue(EmailValidationUtil.MAX_LOCAL_PART_LENGTH)
                .required(false)
                .add().build();
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        Set<ValidationError> errors = new LinkedHashSet<>();
        if (config != null && config.containsKey(MAX_LOCAL_PART_LENGTH_PROPERTY)) {
            Integer maxLocalPartLength = config.getInt(MAX_LOCAL_PART_LENGTH_PROPERTY);
            if (maxLocalPartLength == null || maxLocalPartLength <= 0) {
                errors.add(new ValidationError(ID, MAX_LOCAL_PART_LENGTH_PROPERTY, ValidatorConfigValidator.MESSAGE_CONFIG_INVALID_NUMBER_VALUE, config.get(MAX_LOCAL_PART_LENGTH_PROPERTY)));
            }
        }
        return new ValidationResult(errors);
    }
}
