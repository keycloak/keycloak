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

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Email format validation - accepts plain string and collection of strings, for basic behavior like null/blank values
 * handling and collections support see {@link AbstractStringValidator}.
 */
public class EmailValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final String ID = "email";

    public static final EmailValidator INSTANCE = new EmailValidator();

    public static final String MESSAGE_INVALID_EMAIL = "error-invalid-email";

    // Actually allow same emails like angular. See ValidationTest.testEmailValidation()
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*");

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_EMAIL, value));
        }
    }
    
    @Override
    public String getHelpText() {
        return "Email format validator";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
