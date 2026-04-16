/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.convert;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.KeycloakSession;

/**
 * Holds contextual information available to a {@link Converter} during conversion.
 */
public class ConverterContext {

    /**
     * Holds the {@link KeycloakSession} in which the conversion is performed.
     */
    private final KeycloakSession session;

    /**
     * Holds optional attributes that should be available to {@link Converter} implementations.
     */
    private final Map<String, Object> attributes;

    /**
     * Creates a new {@link ConverterContext} without a {@link KeycloakSession}.
     */
    public ConverterContext() {
        this(null);
    }

    /**
     * Creates a new {@link ConverterContext} with a {@link KeycloakSession}.
     *
     * @param session
     */
    public ConverterContext(KeycloakSession session) {
        this.session = session;
        this.attributes = new HashMap<>();
    }

    /**
     * Eases access to {@link Converter Converter's} for nested conversion.
     *
     * @param converterId
     * @return
     */
    public Converter converter(String converterId) {
        return Converters.converter(session, converterId);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public KeycloakSession getSession() {
        return session;
    }
}
