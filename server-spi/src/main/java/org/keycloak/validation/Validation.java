/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validation;

/**
 * A generic Validation interface.
 */
@FunctionalInterface
public interface Validation {

    String VALIDATION_ERROR = "validation_error";

    /**
     * Validates the given value in the current {@link NestedValidationContext}.
     * Detailed validation problems can be reported via the {@link ValidationProblem} list.
     *
     * @param key     key of the attribute to validate
     * @param value   the value to validate
     * @param context the {@link ValidationContext}
     * @return {@literal true} if the validation succeeded, {@literal false} otherwise.
     */
    boolean validate(ValidationKey key, Object value, NestedValidationContext context);

    /**
     * Tells if the validation is supported in the given {@link ValidationContext} for the given {@code value}.
     *
     * @param key
     * @param context
     * @param value
     * @return
     */
    default boolean isSupported(ValidationKey key, Object value, ValidationContext context) {
        return true;
    }

    /**
     * Function interface to check if the current {@link Validation} is supported in the given {@link ValidationContext}.
     *
     * @see #isSupported(ValidationKey, Object, ValidationContext)
     */
    @FunctionalInterface
    interface ValidationSupported {

        ValidationSupported ALWAYS = (key, value, context) -> true;

        /**
         * @param key
         * @param value
         * @param context
         * @return
         */
        boolean test(ValidationKey key, Object value, ValidationContext context);
    }
}
