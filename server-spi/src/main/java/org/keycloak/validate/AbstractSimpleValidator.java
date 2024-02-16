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

/**
 * Base class for arbitrary value type validators. Functionality covered in this base class:
 * <ul>
 * <li>accepts supported type, collection of supported type.
 * <li>behavior around null and empty values is controlled by {@link #IGNORE_EMPTY_VALUE} configuration option which is
 * boolean. Error should be produced for them by default, but they should be ignored if that option is
 * <code>true</code>. Logic must be implemented in {@link #skipValidation(Object, ValidatorConfig)}.
 * </ul>
 * 
 * @author Vlastimil Elias <velias@redhat.com>
 *
 */
public abstract class AbstractSimpleValidator implements SimpleValidator {

    /**
     * Config option which allows to switch validator to ignore null, empty string and even blank string value - not to
     * produce error for them. Used eg. in UserProfile where we have optional attributes and required concern is checked
     * by separate validators.
     */
    public static final String IGNORE_EMPTY_VALUE = "ignore.empty.value";

    @Override
    public ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (input instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> values = (Collection<Object>) input;

            for (Object value : values) {
                validate(value, inputHint, context, config);
            }

            return context;
        }

        if (skipValidation(input, config)) {
            return context;
        }

        doValidate(input, inputHint, context, config);

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
     * 
     * @see #skipValidation(Object, ValidatorConfig)
     */
    protected abstract void doValidate(Object value, String inputHint, ValidationContext context, ValidatorConfig config);

    /**
     * Decide if validation of individual value should be skipped or not. It should be controlled by
     * {@link #IGNORE_EMPTY_VALUE} configuration option, see {@link #isIgnoreEmptyValuesConfigured(ValidatorConfig)}.
     * 
     * @param value currently validated we make decision for
     * @param config to look for options in
     * @return true if validation should be skipped for this value -
     *         {@link #doValidate(Object, String, ValidationContext, ValidatorConfig)} is not called in this case.
     *         
     * @see #doValidate(Object, String, ValidationContext, ValidatorConfig)         
     */
    protected abstract boolean skipValidation(Object value, ValidatorConfig config);

    /**
     * Default implementation only looks for {@link #IGNORE_EMPTY_VALUE} configuration option.
     * 
     * @param config to get option from
     * @return
     */
    protected boolean isIgnoreEmptyValuesConfigured(ValidatorConfig config) {
        return config != null && config.getBooleanOrDefault(IGNORE_EMPTY_VALUE, false);
    }
}
