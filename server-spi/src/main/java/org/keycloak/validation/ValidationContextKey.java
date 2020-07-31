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

    interface User {

        ValidationContextKey RESOURCE_UPDATE = new BuiltInValidationContextKey("user.resource_update");

        ValidationContextKey PROFILE_UPDATE = new BuiltInValidationContextKey("user.profile_update");

        ValidationContextKey REGISTRATION = new BuiltInValidationContextKey("user.registration");

        ValidationContextKey PROFILE_UPDATE_REGISTRATION = new BuiltInValidationContextKey("user.profile_update_registration");

        ValidationContextKey PROFILE_UPDATE_IDP_REVIEW = new BuiltInValidationContextKey("user.profile_update_idp_review");

        List<ValidationContextKey> ALL_KEYS = Collections.unmodifiableList(Arrays.asList(RESOURCE_UPDATE, PROFILE_UPDATE, PROFILE_UPDATE_IDP_REVIEW, PROFILE_UPDATE_REGISTRATION, REGISTRATION));
    }

    String name();

    static ValidationContextKey newCustomValidationContextKey(String name) {
        return new CustomValidationContextKey(name);
    }

    static ValidationContextKey lookup(String name) {

        for (ValidationContextKey key : User.ALL_KEYS) {
            if (key.name().equals(name)) {
                return key;
            }
        }

        return null;
    }

    final class BuiltInValidationContextKey extends AbstractValidationContextKey {

        public BuiltInValidationContextKey(String name) {
            super(name);
        }
    }

    final class CustomValidationContextKey extends AbstractValidationContextKey {

        public CustomValidationContextKey(String name) {
            super(name);
        }
    }

    class AbstractValidationContextKey implements ValidationContextKey {

        private final String name;

        public AbstractValidationContextKey(String name) {
            this.name = name;
        }

        public String name() {
            return name;
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
