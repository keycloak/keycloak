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
package org.keycloak.validate;

import java.util.Collection;

import org.keycloak.validate.validators.NotBlankValidator;
import org.keycloak.validate.validators.NotEmptyValidator;

/**
 * Base class for arbitrary value type validators. Functionality covered in this base class:
 * <ul>
 * <li>accepts supported type, collection of supported type.
 * <li>null values are always treated as valid to support optional fields! Use other validators (like
 * {@link NotBlankValidator} or {@link NotEmptyValidator} to force field as required.
 * </ul>
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public abstract class AbstractSimpleValidator implements SimpleValidator {

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {

        if (input != null) {
            if (input instanceof Collection) {
                @SuppressWarnings("unchecked")
                Collection<Object> values = (Collection<Object>) input;

                if (values.isEmpty()) {
                    return context;
                }

                for (Object value : values) {
                    validate(value, inputHint, context, config);
                }
            } else {
                doValidate(input, inputHint, context, config);
            }
        }
        return context;
    }

    /**
     * Validate type, format, range of the value etc. Always use {@link ValidationContext#addError(ValidationError)} to
     * report error to the user! Can be called multiple time for one validation if input is Collection.
     * 
     * @param value to be validated, never null
     * @param inputHint
     * @param context for the validation. Add errors into it.
     * @param config of the validation if provided
     */
    protected abstract void doValidate(Object value, String inputHint, ValidationContext context, ValidatorConfig config);
}
