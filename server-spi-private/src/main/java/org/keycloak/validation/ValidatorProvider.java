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

import org.keycloak.provider.Provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A {@link ValidatorProvider} validates a given value in the given {@link ValidationContext} according to validation rules.
 */
public interface ValidatorProvider extends Provider {

    /**
     * Validates a given value in the given {@link ValidationContext} according to validation rules
     * references by the given validation keys.
     *
     * @param context the {@link ValidationContext}
     * @param value   the value to validate
     * @param keys    the keys of the validators to use
     * @return the {@link ValidationResult} with the validation outcome
     */
    ValidationResult validate(ValidationContext context, Object value, Set<ValidationKey> keys);

    default ValidationResult validate(ValidationContext context, Object value, ValidationKey... keys) {
        return validate(context, value, new LinkedHashSet<>(Arrays.asList(keys)));
    }

    default ValidationResult validate(ValidationContext context, Object value, ValidationKey key) {
        return validate(context, value, Collections.singleton(key));
    }

    default ValidationResult validate(ValidationContext context, Object value) {
        return validate(context, value, Collections.emptySet());
    }

    @Override
    default void close() {
        // NOOP
    }
}
