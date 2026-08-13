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
import java.util.regex.Pattern;

import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.validate.AbstractStringValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * This validator disallowing bunch of characters we really not to expect in names of persons (fist, middle, last names).
 * <p>
 * Validates against hardcoded RegEx pattern - accepts plain string and collection of strings, for basic behavior
 * like null/blank values handling and collections support see {@link AbstractStringValidator}.
 */
public class PersonNameProhibitedCharactersValidator extends AbstractStringValidator implements ConfiguredProvider {

    public static final String ID = "person-name-prohibited-characters";

    public static final PersonNameProhibitedCharactersValidator INSTANCE = new PersonNameProhibitedCharactersValidator();

    protected static final Pattern PATTERN = Pattern.compile("^[^<>&\"\\v$%!#?§;*~/\\\\|^=\\[\\]{}()\\p{Cntrl}]+$");
    
    public static final String MESSAGE_NO_MATCH = "error-person-name-invalid-character";
    
    public static final String CFG_ERROR_MESSAGE = "error-message";
    
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
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (!PATTERN.matcher(value).matches()) {
            context.addError(new ValidationError(ID, inputHint, config.getStringOrDefault(CFG_ERROR_MESSAGE, MESSAGE_NO_MATCH)));
        }
    }
   
    
    @Override
    public String getHelpText() {
        return "Basic person name (First, Middle, Last name) validator disallowing bunch of characters we really do not expect in names.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

}
