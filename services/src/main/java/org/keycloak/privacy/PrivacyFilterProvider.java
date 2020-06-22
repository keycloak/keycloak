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
package org.keycloak.privacy;

import org.keycloak.events.Event;
import org.keycloak.provider.Provider;

/**
 * Allows to filter input values that might contain sensitive personally identifiable information (PII).
 *
 * @author <a href="mailto:thomas.darimont@googlemail.com">Thomas Darimont</a>
 */
public interface PrivacyFilterProvider extends Provider {

    /**
     * Filter the given input value according to rules governed by the given type hint.
     *
     * @param field
     * @param input
     * @return the potentially filtered input value
     */
    default String filter(String field, String input) {
        return filter(input);
    }

    /**
     * Filter the given input value according to rules governed by the given type hint in the context of an {@link Event}.
     *
     * @param field
     * @param input
     * @param key   event detail key
     * @param event the keycloak event
     * @return the potentially filtered input value
     */
    default String filter(String field, String input, String key, Event event) {
        return filter(input);
    }

    /**
     * Filter the given input value according to rules governed by this {@link PrivacyFilterProvider}.
     *
     * @param input
     * @return the potentially filtered input value
     */
    String filter(String input);

    /**
     * Potentially releases resources held by this provider.
     */
    default void close() {
        // NOOP
    }
}
