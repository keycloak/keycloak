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

import java.util.List;

import org.keycloak.services.validation.Validation;
import org.keycloak.userprofile.AttributeContext;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;
import org.keycloak.validate.ValidatorConfig.ValidatorConfigBuilder;

/**
 * Validator to check that User Profile attribute value is not blank (null value is OK!). Expects List of Strings as
 * input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class BlankAttributeValidator implements SimpleValidator {

    public static final String ID = "up-blank-attribute-value";

    public static final String CFG_ERROR_MESSAGE = "error-message";

    public static final String CFG_FAIL_ON_NULL = "fail-on-null";
    
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        boolean failOnNull = config.getBooleanOrDefault(CFG_FAIL_ON_NULL, false);
        
        if (values.isEmpty() && !failOnNull) {
            return context;
        }

        AttributeContext attributeContext = UserProfileAttributeValidationContext.from(context).getAttributeContext();

        if (!attributeContext.getMetadata().isRequired(attributeContext)) {
            return context;
        }

        String value = values.isEmpty() ? null: values.get(0);

        if ((failOnNull || value != null) && Validation.isBlank(value)) {
            context.addError(new ValidationError(ID, inputHint, config.getStringOrDefault(CFG_ERROR_MESSAGE, AttributeRequiredByMetadataValidator.ERROR_USER_ATTRIBUTE_REQUIRED)));
        }

        return context;
    }

    /**
     * Create config for this validator to get customized error message
     * 
     * @param errorMessage to be used if validation fails
     * @param failOnNull makes validator fail on null values also (not on empty string only as is the default behavior)
     * @return config
     */
    public static ValidatorConfig createConfig(String errorMessage, boolean failOnNull) {
        ValidatorConfigBuilder builder = ValidatorConfig.builder();
        builder.config(CFG_FAIL_ON_NULL, failOnNull);
        if (errorMessage != null) {
            builder.config(CFG_ERROR_MESSAGE, errorMessage);
        }
        return builder.build();
    }

}
