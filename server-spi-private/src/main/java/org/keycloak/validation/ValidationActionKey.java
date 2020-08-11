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
 * Denotes the action in which the validation takes place, e.g. create or update.
 */
public interface ValidationActionKey {

    /**
     * The default {@link ValidationActionKey} if none of create or update is suitable.
     */
    ValidationActionKey DEFAULT = new BuiltinActionKey("default");

    /**
     * Used for validations during create operations.
     */
    ValidationActionKey CREATE = new BuiltinActionKey("create");

    /**
     * Used for validations during update operations.
     */
    ValidationActionKey UPDATE = new BuiltinActionKey("update");

    /**
     * Used for validations during delete operations.
     */
    ValidationActionKey DELETE = new BuiltinActionKey("delete");

    List<ValidationActionKey> ALL_KEYS = Collections.unmodifiableList(Arrays.asList(
            DEFAULT,
            CREATE,
            UPDATE,
            DELETE
    ));

    /**
     * The name of the {@link ValidationActionKey}
     *
     * @return
     */
    String getName();

    /**
     * Looks for built-in {@link ValidationActionKey} instance with the given name.
     *
     * @param name
     * @return
     */
    static ValidationActionKey get(String name) {
        return AbstractActionKey.Internal.CACHE.get(name);
    }

    /**
     * Returns a built-in {@link ValidationActionKey.BuiltinActionKey} if present or creates a new {@link ValidationActionKey.CustomActionKey}.
     * <p>
     *
     * @param name
     * @return
     */
    static ValidationActionKey getOrCreate(String name) {
        ValidationActionKey key = get(name);
        return key != null ? key : new CustomActionKey(name);
    }

    /**
     * Keycloak built-in {@link ValidationActionKey}.
     * <p>
     * This type should only be used for Keycloak Internal {@link ValidationActionKey} implementations.
     * Users who want to define custom ContextActionKeys should use the {@link CustomActionKey}.
     */
    final class BuiltinActionKey extends AbstractActionKey {

        public BuiltinActionKey(String name) {
            super(name);
        }
    }

    /**
     * Custom {@link ValidationKey}.
     * <p>
     * This is meant for users who want to define custom {@link ValidationActionKey}.
     */
    final class CustomActionKey extends AbstractActionKey {

        public CustomActionKey(String name) {
            super(name);
        }
    }

    /**
     * This type should be used for custom {@link ValidationActionKey} implementations.
     */
    abstract class AbstractActionKey implements ValidationActionKey {

        /**
         * Lazy static singleton holder for the {@link AbstractActionKey.Internal#CACHE}.
         */
        static class Internal {

            private static final Map<String, ValidationActionKey> CACHE;

            static {
                Map<String, ValidationActionKey> map = new HashMap<>();
                for (ValidationActionKey key : ALL_KEYS) {
                    map.put(key.getName(), key);
                }
                CACHE = Collections.unmodifiableMap(map);
            }
        }

        private final String name;

        public AbstractActionKey(String name) {
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
            if (!(o instanceof AbstractActionKey)) {
                return false;
            }
            AbstractActionKey that = (AbstractActionKey) o;
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
