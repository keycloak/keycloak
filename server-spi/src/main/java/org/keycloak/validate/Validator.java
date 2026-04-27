/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.validate;

import java.util.Map;

import org.keycloak.provider.Provider;

/**
 * Validates given input in a {@link ValidationContext}.
 * <p>
 * Validations can be supported with an optional {@code inputHint}, which could denote a reference to a potentially
 * nested attribute of an object to validate.
 * <p>
 * Validations can be configured with an optional {@code config} {@link Map}.
 */
public interface Validator extends Provider {

    /**
     * Validates the given {@code input}.
     *
     * @param input the value to validate
     * @return the validation context with the outcome of the validation
     */
    default ValidationContext validate(Object input) {
        return validate(input, "input", new ValidationContext(), ValidatorConfig.EMPTY);
    }

    /**
     * Validates the given {@code input}.
     *
     * @param input   the value to validate
     * @param context the validation context
     * @return the validation context with the outcome of the validation
     */
    default ValidationContext validate(Object input, ValidationContext context) {
        return validate(input, "input", context, ValidatorConfig.EMPTY);
    }

    /**
     * Validates the given {@code input} with an additional {@code inputHint}.
     *
     * @param input     the value to validate
     * @param inputHint an optional input hint to guide the validation
     * @return the validation context with the outcome of the validation
     */
    default ValidationContext validate(Object input, String inputHint) {
        return validate(input, inputHint, new ValidationContext(), ValidatorConfig.EMPTY);
    }

    /**
     * Validates the given {@code input} with an additional {@code inputHint}.
     *
     * @param input     the value to validate
     * @param inputHint an optional input hint to guide the validation
     * @param config    parameterization for the current validation
     * @return the validation context with the outcome of the validation
     */
    default ValidationContext validate(Object input, String inputHint, ValidatorConfig config) {
        return validate(input, inputHint, new ValidationContext(), config);
    }

    /**
     * Validates the given {@code input} with an additional {@code inputHint}.
     *
     * @param input     the value to validate
     * @param inputHint an optional input hint to guide the validation
     * @param context   the validation context
     * @return the validation context with the outcome of the validation
     */
    default ValidationContext validate(Object input, String inputHint, ValidationContext context) {
        return validate(input, inputHint, context, ValidatorConfig.EMPTY);
    }

    /**
     * Validates the given {@code input} with an additional {@code inputHint} and {@code config}.
     *
     * @param input     the value to validate
     * @param inputHint an optional input hint to guide the validation
     * @param context   the validation context
     * @param config    parameterization for the current validation
     * @return the validation context with the outcome of the validation
     */
    ValidationContext validate(Object input, String inputHint, ValidationContext context, ValidatorConfig config);

    default void close() {
        // NOOP
    }
}
