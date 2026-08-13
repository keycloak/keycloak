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

import java.util.Collection;

import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

/**
 * Validate that value exists and is not empty nor blank. Supports String and collection of Strings as input. For
 * collection of Strings input has to contain at least one element and it have to be non-blank to satisfy this
 * validation. If collection contains something else than String, or if even one String in it is blank, then this
 * validation fails.
 * 
 * @see NotEmptyValidator
 */
public class NotBlankValidator implements SimpleValidator {

    public static final String ID = "not-blank";

    public static final String MESSAGE_BLANK = "error-invalid-blank";

    public static final NotBlankValidator INSTANCE = new NotBlankValidator();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input == null) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_BLANK, input));
        } else if (input instanceof String) {
            validateStringValue((String) input, inputHint, context, config);
        } else if (input instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> values = (Collection<Object>) input;
            if (!values.isEmpty()) {
                for (Object value : values) {
                    if (!(value instanceof String)) {
                        context.addError(new ValidationError(getId(), inputHint, ValidationError.MESSAGE_INVALID_VALUE, input));
                        return context;
                    } else if (!validateStringValue((String) value, inputHint, context, config)) {
                        return context;
                    }
                }
            } else {
                context.addError(new ValidationError(ID, inputHint, MESSAGE_BLANK, input));
            }
        } else {
            context.addError(new ValidationError(ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE, input));
        }

        return context;
    }

    protected boolean validateStringValue(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (value == null || value.trim().length() == 0) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_BLANK, value));
            return false;
        }
        return true;
    }
}
