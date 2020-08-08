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
 * Denotes a dedicated ValidationContext in which certain {@link Validation} rules should be applied, e.g. during User Registration,
 * User Profile change, Client Registration, Realm Definition, Identity Provider Configuration, etc.
 * {@link Validation Validation's} can be associated with different ValidationContextKey.
 * <p>
 * Users can create custom {@link ValidationContextKey ValidationContextKey's} by implementing this interface.
 * It is recommended that custom {@link ValidationContextKey} implementations are singletons.
 */
public interface ValidationContextKey {

    ValidationContextKey KEYCLOAK_DEFAULT_CONTEXT_KEY = new BuiltInValidationContextKey("", null);

    interface User {

        ValidationContextKey USER_DEFAULT_CONTEXT_KEY =
                new BuiltInValidationContextKey("user", ValidationContextKey.KEYCLOAK_DEFAULT_CONTEXT_KEY);

        ValidationContextKey USER_RESOURCE_UPDATE_CONTEXT_KEY =
                new BuiltInValidationContextKey("user.resource_update", User.USER_DEFAULT_CONTEXT_KEY);

        ValidationContextKey USER_PROFILE_UPDATE_CONTEXT_KEY =
                new BuiltInValidationContextKey("user.profile_update", User.USER_DEFAULT_CONTEXT_KEY);

        ValidationContextKey USER_REGISTRATION_CONTEXT_KEY =
                new BuiltInValidationContextKey("user.registration", User.USER_DEFAULT_CONTEXT_KEY);

        ValidationContextKey USER_PROFILE_UPDATE_REGISTRATION_CONTEXT_KEY =
                new BuiltInValidationContextKey("user.profile_update_registration", User.USER_DEFAULT_CONTEXT_KEY);

        ValidationContextKey USER_PROFILE_UPDATE_IDP_REVIEW_CONTEXT_KEY =
                new BuiltInValidationContextKey("user.profile_update_idp_review", User.USER_DEFAULT_CONTEXT_KEY);

        List<ValidationContextKey> USER_ALL_CONTEXT_KEYS = Collections.unmodifiableList(
                Arrays.asList(USER_DEFAULT_CONTEXT_KEY, USER_RESOURCE_UPDATE_CONTEXT_KEY, USER_PROFILE_UPDATE_CONTEXT_KEY,
                        USER_PROFILE_UPDATE_IDP_REVIEW_CONTEXT_KEY, USER_PROFILE_UPDATE_REGISTRATION_CONTEXT_KEY, USER_REGISTRATION_CONTEXT_KEY));
    }

    String getName();

    ValidationContextKey getParent();

    static ValidationContextKey newCustomValidationContextKey(String name, ValidationContextKey parent) {
        return new CustomValidationContextKey(name, parent);
    }

    static ValidationContextKey lookup(String name) {

        for (ValidationContextKey key : User.USER_ALL_CONTEXT_KEYS) {
            if (key.getName().equals(name)) {
                return key;
            }
        }

        if (KEYCLOAK_DEFAULT_CONTEXT_KEY.getName().equals(name)) {
            return KEYCLOAK_DEFAULT_CONTEXT_KEY;
        }

        return null;
    }

    final class BuiltInValidationContextKey extends AbstractValidationContextKey {

        public BuiltInValidationContextKey(String name, ValidationContextKey parent) {
            super(name, parent);
        }
    }

    final class CustomValidationContextKey extends AbstractValidationContextKey {

        public CustomValidationContextKey(String name, ValidationContextKey parent) {
            super(name, parent);
        }
    }

    class AbstractValidationContextKey implements ValidationContextKey {

        private final String name;

        private final ValidationContextKey parent;

        public AbstractValidationContextKey(String name, ValidationContextKey parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        @Override
        public ValidationContextKey getParent() {
            return parent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AbstractValidationContextKey)) return false;
            AbstractValidationContextKey that = (AbstractValidationContextKey) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
