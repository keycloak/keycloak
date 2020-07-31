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
import org.keycloak.validation.ValidationRegistry.MutableValidationRegistry;

/**
 * Provides custom {@link Validation Validation's} to a given {@link ValidationRegistry}.
 */
public interface ValidationProvider extends Provider {

    /**
     * Registers new {@link Validation} implementations into the given {@link ValidationRegistry}.
     *
     * @param registry to store the new {@link Validation Validation's}.
     */
    void register(MutableValidationRegistry registry);

    default void close() {
        // NOOP
    }
}
