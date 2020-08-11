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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A registry for {@link Validation Validation's}.
 */
public interface ValidationRegistry {

    /**
     * The default order for a validation within a {@link ValidationKey} scoped set of validations.
     */
    double DEFAULT_ORDER = 0.0;

    /**
     * Returns all {@link Validation Validation's} registered for the given Set of validation keys.
     *
     * @param keys
     * @return Map with validation key as key and the List of associated {@link Validation Validation's}.
     */
    Map<ValidationKey, List<NamedValidation>> getValidations(Set<ValidationKey> keys);

    /**
     * Returns all {@link Validation Validation's} registered for the given validation key.
     *
     * @param key
     * @return
     */
    List<NamedValidation> getValidations(ValidationKey key);

    /**
     * Returns all {@link Validation}s, that are eligible for the given validation context keys
     * and {@link ValidationContext} as well as the given value.
     *
     * @param context
     * @param keys
     * @param value
     * @return Map with validation key as key and the List of associated {@link Validation Validation's}.
     */
    Map<ValidationKey, List<NamedValidation>> resolveValidations(ValidationContext context, Set<ValidationKey> keys, Object value);

    /**
     * Returns all {@link Validation}s, that are eligible for the given validation context key
     * and {@link ValidationContext} as well as the given value.
     *
     * @param context
     * @param key
     * @param value
     * @return
     */
    List<NamedValidation> resolveValidations(ValidationContext context, ValidationKey key, Object value);

    interface MutableValidationRegistry extends ValidationRegistry {

        /**
         * Registers a new {@link Validation} for the given {@link ValidationKey} that can be applied in the given validation context keys.
         *
         * @param key         the key referencing the validation target
         * @param validation  the validation logic
         * @param name        a logical name for the new {@link Validation}
         * @param contextKeys controls in which context the given {@link Validation} should be considered
         */
        void addValidation(ValidationKey key, Validation validation, String name, Set<ValidationContextKey> contextKeys);

        /**
         * Adds a new {@link Validation} for the given {@link ValidationKey} that can be applied in the given validation context keys.
         *
         * @param key         the key referencing the validation target
         * @param validation  the validation logic
         * @param name        a logical name for the new {@link Validation}
         * @param contextKeys controls in which context the given {@link Validation} should be considered
         */
        default void addValidation(ValidationKey key, Validation validation, String name, ValidationContextKey... contextKeys) {
            addValidation(key, validation, name, new LinkedHashSet<>(Arrays.asList(contextKeys)));
        }

        /**
         * Inserts a new {@link Validation} for the given {@link ValidationKey} that can be applied in the given validation context keys at the position indicated by the given order.
         * Note that this can be used to replace existing validations.
         *
         * @param key
         * @param validation
         * @param order
         * @param name
         * @param contextKeys
         */
        void insertValidation(ValidationKey key, Validation validation, Double order, String name, Set<ValidationContextKey> contextKeys);

        /**
         * Inserts a new {@link Validation} for the given {@link ValidationKey} that can be applied in the given validation context keys at the position indicated by the given order.
         * Note that this can be used to replace existing validations.
         *
         * @param key
         * @param validation
         * @param order
         * @param name
         * @param contextKeys
         */
        default void insertValidation(ValidationKey key, Validation validation, Double order, String name, ValidationContextKey... contextKeys) {
            insertValidation(key, validation, order, name, new LinkedHashSet<>(Arrays.asList(contextKeys)));
        }
    }
}
