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

    // Note: fields are defined as strings instead of an enum, to ease adding own fields in custom implementations.

    // The following fields can be used by consumers to classify and filer their own PII values.

    /**
     * Denotes a USER_ID type
     */
    String USER_ID = "userId";

    /**
     * Denotes an USERNAME type
     */
    String USERNAME = "username";

    /**
     * Denotes an NAME type, e.g. a given name, family name.
     */
    String NAME = "name";

    /**
     * Denotes an EMAIL address type
     */
    String EMAIL = "email";

    /**
     * Denotes an PHONE_NUMBER type, e.g. mobile, phone
     */
    String PHONE_NUMBER = "phoneNumber";

    /**
     * Denotes an ADDRESS type
     */
    String ADDRESS = "address";

    /**
     * Denotes an PII type for generic personally identifiable information.
     */
    String PII = "pii";

    /**
     * Denotes an IP_ADDRESS type
     */
    String IP_ADDRESS = "ipAddress";

    /**
     * Denotes an unspecified default type
     */
    String DEFAULT = "default";

    /**
     * Filter the given input value according to rules governed by the given type hint in the context of an {@link Event}.
     *
     * @param input
     * @param typeHint
     * @param userEvent the keycloak event, may be null.
     * @return the potentially filtered input value
     */
    String filter(String input, String typeHint, Event userEvent);

    /**
     * Filter the given input value according to rules governed by the given type hint.
     *
     * @param input
     * @param typeHint
     * @return the potentially filtered input value
     */
    default String filter(String input, String typeHint) {
        return filter(input, typeHint, null);
    }

    /**
     * Filter the given input value according to rules governed by this {@link PrivacyFilterProvider}.
     *
     * @param input
     * @return the potentially filtered input value
     */
    default String filter(String input) {
        return filter(input, DEFAULT, null);
    }

    /**
     * Potentially releases resources held by this provider.
     */
    default void close() {
        // NOOP
    }
}
