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
package org.keycloak.validate.builtin;

import org.keycloak.validate.CompactValidator;
import org.keycloak.validate.ValidationContext;
import org.keycloak.validate.ValidationError;
import org.keycloak.validate.ValidatorConfig;

public class NumberValidator implements CompactValidator {

    public static final String ID = "number";

    public static final String MESSAGE_INVALID_NUMBER = "error-invalid-number";

    public static final NumberValidator INSTANCE = new NumberValidator();

    private NumberValidator() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input instanceof Number) {
            return context;
        }

        if (input instanceof String) {
            // if we have a String then check if it represents a valid number.

            // try to parse the string into a number
            String string = (String) input;
            try {
                Integer.parseInt(string);
                // okay we have an integer
            } catch (NumberFormatException nfe) {
                try {
                    Double.parseDouble(string);
                    // okay we have a double
                } catch (NumberFormatException nfe2) {
                    context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_NUMBER, input));
                }
            }

            return context;
        }

        context.addError(new ValidationError(ID, inputHint, MESSAGE_INVALID_NUMBER, input));

        return context;
    }
}
