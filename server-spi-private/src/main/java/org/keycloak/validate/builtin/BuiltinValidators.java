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
import org.keycloak.validate.Validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A registry of builtin {@link Validator} implementations.
 */
public class BuiltinValidators {

    private static final Map<String, Validator> INTERNAL_VALIDATORS;

    static {
        List<CompactValidator> list = Arrays.asList(
                LengthValidator.INSTANCE,
                NotEmptyValidator.INSTANCE,
                UriValidator.INSTANCE,
                EmailValidator.INSTANCE,
                NotBlankValidator.INSTANCE,
                PatternValidator.INSTANCE,
                NumberValidator.INSTANCE
        );

        Map<String, Validator> validators = new HashMap<>();

        for (CompactValidator validator : list) {
            validators.put(validator.getId(), validator);
        }

        INTERNAL_VALIDATORS = validators;
    }

    public static Validator getValidatorById(String id) {
        return INTERNAL_VALIDATORS.get(id);
    }

    public static Map<String, Validator> getValidators() {
        return Collections.unmodifiableMap(INTERNAL_VALIDATORS);
    }

    public static NotBlankValidator notBlank() {
        return NotBlankValidator.INSTANCE;
    }

    public static NotEmptyValidator notEmpty() {
        return NotEmptyValidator.INSTANCE;
    }

    public static LengthValidator length() {
        return LengthValidator.INSTANCE;
    }

    public static UriValidator uri() {
        return UriValidator.INSTANCE;
    }

    public static EmailValidator email() {
        return EmailValidator.INSTANCE;
    }

    public static PatternValidator pattern() {
        return PatternValidator.INSTANCE;
    }

    public static NumberValidator number() {
        return NumberValidator.INSTANCE;
    }
}