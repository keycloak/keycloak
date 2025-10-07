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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidationResult;
import org.keycloak.validate.ValidatorConfig;

/**
 * A date validator that only takes into account the format associated with the current locale.
 */
public class LocalDateValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final LocalDateValidator INSTANCE = new LocalDateValidator();

    public static final String ID = "local-date";

    public static final String MESSAGE_INVALID_DATE = "error-invalid-date";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        UserModel user = (UserModel) context.getAttributes().get(UserModel.class.getName());
        KeycloakSession session = context.getSession();
        KeycloakContext keycloakContext = session.getContext();
        Locale locale = keycloakContext.resolveLocale(user);
        DateFormat formatter = DateFormat.getDateInstance(DateFormat.SHORT, locale);

        formatter.setLenient(false);

        try {
            formatter.parse(value);
        } catch (ParseException e) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_DATE));
        }
    }

    @Override
    public ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        return ValidationResult.OK;
    }

    @Override
    public String getHelpText() {
        return "Validates date formats based on the realm or user locale.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    protected boolean isIgnoreEmptyValuesConfigured(ValidatorConfig config) {
        return true;
    }
}
