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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
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

    ValidationContextKey DEFAULT_CONTEXT_KEY = new BuiltInValidationContextKey("", null);

    ValidationContextKey USER_DEFAULT_CONTEXT_KEY =
            new BuiltInValidationContextKey("user", ValidationContextKey.DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_RESOURCE_UPDATE_CONTEXT_KEY =
            new BuiltInValidationContextKey("resource_update", USER_DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_PROFILE_UPDATE_CONTEXT_KEY =
            new BuiltInValidationContextKey("profile_update", USER_DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_REGISTRATION_CONTEXT_KEY =
            new BuiltInValidationContextKey("registration", USER_DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_PROFILE_UPDATE_REGISTRATION_CONTEXT_KEY =
            new BuiltInValidationContextKey("profile_update_registration", USER_DEFAULT_CONTEXT_KEY);

    ValidationContextKey USER_PROFILE_UPDATE_IDP_REVIEW_CONTEXT_KEY =
            new BuiltInValidationContextKey("profile_update_idp_review", USER_DEFAULT_CONTEXT_KEY);

    List<ValidationContextKey> ALL_CONTEXT_KEYS = Collections.unmodifiableList(Arrays.asList(

            DEFAULT_CONTEXT_KEY,

            USER_DEFAULT_CONTEXT_KEY,
            USER_RESOURCE_UPDATE_CONTEXT_KEY,
            USER_PROFILE_UPDATE_CONTEXT_KEY,
            USER_PROFILE_UPDATE_IDP_REVIEW_CONTEXT_KEY,
            USER_PROFILE_UPDATE_REGISTRATION_CONTEXT_KEY,
            USER_REGISTRATION_CONTEXT_KEY
    ));


    String getName();

    ValidationContextKey getParent();

    static ValidationContextKey newCustomValidationContextKey(String suffix, ValidationContextKey parent) {
        return new CustomValidationContextKey(suffix, parent);
    }

    static ValidationContextKey lookup(String name) {

        for (ValidationContextKey key : ALL_CONTEXT_KEYS) {
            if (key.getName().equals(name)) {
                return key;
            }
        }

        return null;
    }

    final class BuiltInValidationContextKey extends AbstractValidationContextKey {

        public BuiltInValidationContextKey(String suffix, ValidationContextKey parent) {
            super(suffix, parent);
        }
    }

    final class CustomValidationContextKey extends AbstractValidationContextKey {

        public CustomValidationContextKey(String suffix, ValidationContextKey parent) {
            super(suffix, parent);
        }
    }

    class AbstractValidationContextKey implements ValidationContextKey {

        private final String name;

        private final ValidationContextKey parent;

        public AbstractValidationContextKey(String suffix, ValidationContextKey parent) {
            this.name = createName(parent, suffix);
            this.parent = parent;
        }

        private String createName(ValidationContextKey parent, String suffix) {

            if (parent == null) {
                return "";
            }

            Deque<ValidationContextKey> deque = new ArrayDeque<>();
            ValidationContextKey current = parent;
            do {
                if (DEFAULT_CONTEXT_KEY.equals(current)) {
                    break;
                }
                deque.add(current);
                current = current.getParent();
            } while (current != null);


            StringBuilder path = new StringBuilder();
            while (!deque.isEmpty()) {
                path.append(deque.removeLast().getName()).append('.');
            }

            path.append(suffix);

            return path.toString();
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
