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
import org.keycloak.userprofile.AttributeMetadata;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validator to check that User Profile attribute value is not blank (nor null) if the attribute is required based on
 * AttributeMetadata predicate. Expects List of Strings as input.
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public class AttributeRequiredByMetadataValidator implements SimpleValidator {

    public static final String ERROR_USER_ATTRIBUTE_REQUIRED = "error-user-attribute-required";

    public static final String ID = "up-attribute-required-by-metadata-value";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        AttributeContext attContext = UserProfileAttributeValidationContext.from(context).getAttributeContext();
        AttributeMetadata metadata = attContext.getMetadata();

        if (!metadata.isRequired(attContext)) {
            return context;
        }

        if (metadata.isReadOnly(attContext)) {
            return context;
        }

        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) input;

        if (values == null || values.isEmpty()) {
            context.addError(new ValidationError(ID, inputHint, ERROR_USER_ATTRIBUTE_REQUIRED));
        } else {
            for (String value : values) {
                if (Validation.isBlank(value)) {
                    context.addError(new ValidationError(ID, inputHint, ERROR_USER_ATTRIBUTE_REQUIRED));
                    return context;
                }
            }
        }
        return context;
    }
}
