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
import java.util.List;
import java.util.Objects;

/**
 * Denotes a validatable property, e.g. Realm Attributes, User Properties, Client Properties, etc.
 * <p>
 * Users can create custom {@link ValidationKey ValidationKey's} by implementing this interface.
 * It is recommended that custom {@link ValidationKey} implementations are singletons.
 */
public interface ValidationKey {

    BuiltinValidationKey REALM = new BuiltinValidationKey("realm", false);

    BuiltinValidationKey CLIENT = new BuiltinValidationKey("client", false);

    // User Entities
    // USER_PROFILE
    // USER_REGISTRATION
    // USER
    BuiltinValidationKey USER = new BuiltinValidationKey("user", false);
    // User Attributes
    BuiltinValidationKey USER_USERNAME = new BuiltinValidationKey("user.username", true);
    BuiltinValidationKey USER_EMAIL = new BuiltinValidationKey("user.email", true);
    BuiltinValidationKey USER_FIRSTNAME = new BuiltinValidationKey("user.firstName", true);
    BuiltinValidationKey USER_LASTNAME = new BuiltinValidationKey("user.lastName", true);

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

    boolean isPropertyKey();

    /**
     * Create a new {@link ValidationKey}.
     * <p>
     * Note that this is only for internal user and for creation of custom {@link ValidationKey ValidationKeys}.
     *
     * @param name
     * @return
     */
    static CustomValidationKey newCustomKey(String name, boolean propertyKey) {
        return new CustomValidationKey(name, propertyKey);
    }

    /**
     * Looks for an existing {@link ValidationKey} instance with the given name.
     *
     * @param name
     * @return
     */
    static ValidationKey lookup(String name) {

        for (ValidationKey key : ALL_KEYS) {
            if (key.getName().equals(name)) {
                return key;
            }
        }

        return null;
    }

    /**
     * Keycloak built-in {@link ValidationKey}.
     * <p>
     * This type should only be used for Keycloak Internal {@link ValidationKey} implementations.
     * Users who want to define custom ValidationKeys should use the CustomValidationKey.
     */
    final class BuiltinValidationKey extends AbstractValidationKey {

        public BuiltinValidationKey(String name, boolean propertyKey) {
            super(name, propertyKey);
        }
    }

    /**
     * Custom {@link ValidationKey}.
     * <p>
     * This is meant for users who want to define custom ValidationKeys.
     */
    final class CustomValidationKey extends AbstractValidationKey {

        public CustomValidationKey(String name, boolean propertyKey) {
            super(name, propertyKey);
        }
    }

    /**
     * This type should be used for custom {@link ValidationKey} implementations.
     */
    abstract class AbstractValidationKey implements ValidationKey {

        private final String name;

        private final boolean propertyKey;

        public AbstractValidationKey(String name, boolean propertyKey) {
            this.name = name;
            this.propertyKey = propertyKey;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean isPropertyKey() {
            return propertyKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractValidationKey)) return false;
            AbstractValidationKey that = (AbstractValidationKey) o;
            return propertyKey == that.propertyKey &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, propertyKey);
        }
    }
}
