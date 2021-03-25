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

import org.keycloak.provider.Provider;

import java.util.Collections;
import java.util.Map;

/**
 * Validates given input in a {@link ValidationContext}.
 *
 * Validations can be supported with an optional {@code inputHint}, which could denote a reference to a potentially
 * nested attribute of an object to validate.
 *
 * Validations can be configured with an optional {@code config} {@link Map}.
 */
public interface Validator extends Provider {

    /**
     * Validates the given {@code input}.
     *
     * @param input   the value to validate
     * @param context the validation context
     */
    default void validate(Object input, ValidationContext context) {
        validate(input, null, context, Collections.emptyMap());
    }

    /**
     * Validates the given {@code input} with an additional {@code inputHint}.
     *
     * @param input     the value to validate
     * @param inputHint an optional input hint to guide the validation
     * @param context   the validation context
     */
    default void validate(Object input, String inputHint, ValidationContext context) {
        validate(input, inputHint, context, Collections.emptyMap());
    }

    /**
     * Validates the given {@code input} with an additional {@code inputHint} and {@code config}.
     *
     * @param input     the value to validate
     * @param inputHint an optional input hint to guide the validation
     * @param context   the validation context
     * @param config    parameterization for the current validation
     */
    void validate(Object input, String inputHint, ValidationContext context, Map<String, Object> config);

    default void close() {
        // NOOP
    }
}
