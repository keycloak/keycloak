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

import org.keycloak.validate.SimpleValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

import java.util.Collection;
import java.util.Map;

/**
 * Check that input value is not empty. It means it is not null for all data types. For String it also have to be
 * non-empty string (no trim() performed). For {@link Collection} and {@link Map} it also means it is not empty.
 * 
 * @see NotBlankValidator
 */
public class NotEmptyValidator implements SimpleValidator {

    public static final NotEmptyValidator INSTANCE = new NotEmptyValidator();

    public static final String ID = "not-empty";

    public static final String MESSAGE_ERROR_EMPTY = "error-empty";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input == null) {
            context.addError(new ValidationError(ID, inputHint, MESSAGE_ERROR_EMPTY, input));
            return context;
        }

        if (input instanceof String) {
            if (((String) input).length() == 0) {
                context.addError(new ValidationError(ID, inputHint, MESSAGE_ERROR_EMPTY, input));
            }
            return context;
        }

        if (input instanceof Collection) {
            if (((Collection<?>) input).isEmpty()) {
                context.addError(new ValidationError(ID, inputHint, MESSAGE_ERROR_EMPTY, input));
            }
            return context;
        }

        if (input instanceof Map) {
            if (((Map<?, ?>) input).isEmpty()) {
                context.addError(new ValidationError(ID, inputHint, MESSAGE_ERROR_EMPTY, input));
            }
            return context;
        }

        context.addError(new ValidationError(ID, inputHint, ValidationError.MESSAGE_INVALID_VALUE, input));
        return context;
    }
}
