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

import org.keycloak.validate.validators.NotBlankValidator;

/**
 * Base class for String value format validators. Functionality covered in this base class:
 * <ul>
 * <li>accepts plain string and collections of strings as input
 * <li>each item is validated for collections of strings, see
 * {@link #validateFormat(String, String, ValidationContext, ValidatorConfig)}
 * <li>null values are always treated as valid to support optional fields! Use other validators (like
 * {@link NotBlankValidator} to force field as required.
 * </ul>
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public abstract class AbstractStringValidator extends AbstractSimpleValidator {

    @Override
    protected void doValidate(Object value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (value instanceof String) {
            doValidate(value.toString(), inputHint, context, config);
        } else {
            context.addError(new ValidationError(getId(), inputHint, ValidationError.MESSAGE_INVALID_VALUE, value));
        }
    }

    protected abstract void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config);
}
