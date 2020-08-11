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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Denotes a validatable property, e.g. Realm Attributes, User Properties, Client Properties, etc.
 * <p>
 * Users can create custom {@link ValidationKey ValidationKey's} by implementing this interface.
 * It is recommended that custom {@link ValidationKey} implementations are singletons.
 */
public interface ValidationKey {

    BuiltinValidationKey REALM = new BuiltinValidationKey("realm");

    BuiltinValidationKey CLIENT = new BuiltinValidationKey("client");

    // User Entities
    // USER_PROFILE
    // USER_REGISTRATION
    // USER
    BuiltinValidationKey USER = new BuiltinValidationKey("user");
    // User Attributes
    BuiltinValidationKey USER_USERNAME = new BuiltinValidationKey("user.username");
    BuiltinValidationKey USER_EMAIL = new BuiltinValidationKey("user.email");
    BuiltinValidationKey USER_FIRSTNAME = new BuiltinValidationKey("user.firstName");
    BuiltinValidationKey USER_LASTNAME = new BuiltinValidationKey("user.lastName");

    List<BuiltinValidationKey> ALL_KEYS = Collections.unmodifiableList(Arrays.asList(
            REALM,

            CLIENT,

            USER,
            USER_USERNAME,
            USER_EMAIL,
            USER_FIRSTNAME,
            USER_LASTNAME
    ));

    // TODO add additional supported attributes

    /**
     * The name of the {@link ValidationKey}
     *
     * @return
     */
    String getName();

    /**
     * Looks for built-in {@link ValidationKey} instance with the given name.
     *
     * @param name
     * @return
     */
    static ValidationKey get(String name) {
        return AbstractValidationKey.Internal.CACHE.get(name);
    }

    /**
     * Returns a built-in {@link BuiltinValidationKey} if present or creates a new {@link CustomValidationKey}.
     * <p>
     *
     * @param name
     * @return
     */
    static ValidationKey getOrCreate(String name) {
        ValidationKey key = get(name);
        return key != null ? key : new CustomValidationKey(name);
    }

    /**
     * Keycloak built-in {@link ValidationKey}.
     * <p>
     * This type should only be used for Keycloak Internal {@link ValidationKey} implementations.
     * Users who want to define custom ValidationKeys should use the CustomValidationKey.
     */
    final class BuiltinValidationKey extends AbstractValidationKey {

        public BuiltinValidationKey(String name) {
            super(name);
        }
    }

    /**
     * Custom {@link ValidationKey}.
     * <p>
     * This is meant for users who want to define custom ValidationKeys.
     */
    final class CustomValidationKey extends AbstractValidationKey {

        public CustomValidationKey(String name) {
            super(name);
        }
    }

    /**
     * This type should be used for custom {@link ValidationKey} implementations.
     */
    abstract class AbstractValidationKey implements ValidationKey {

        /**
         * Lazy static singleton holder for the {@link Internal#CACHE}.
         */
        static class Internal {

            private static final Map<String, ValidationKey> CACHE;

            static {
                Map<String, ValidationKey> map = new HashMap<>();
                for (ValidationKey key : ALL_KEYS) {
                    map.put(key.getName(), key);
                }
                CACHE = Collections.unmodifiableMap(map);
            }
        }

        private final String name;

        public AbstractValidationKey(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AbstractValidationKey)) {
                return false;
            }
            AbstractValidationKey that = (AbstractValidationKey) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
