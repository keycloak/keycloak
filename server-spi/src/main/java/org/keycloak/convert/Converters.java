/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.convert;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.validate.ValidationResult;

/**
 * Facade for Converter functions with support for {@link Converter} implementation lookup by id.
 */
public class Converters {

    /**
     * Holds the {@link KeycloakSession}.
     */
    private final KeycloakSession session;

    /**
     * Creates a new {@link Converters} instance with the given {@link KeycloakSession}.
     *
     * @param session
     */
    public Converters(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Look-up for a built-in or registered {@link Converter} with the given provider {@code id}.
     *
     * @param id
     * @return
     * @see #converter(KeycloakSession, String)
     */
    public Converter converter(String id) {
        return converter(session, id);
    }

    /**
     * Look-up for a built-in or registered {@link ConverterFactory} with the given provider {@code id}.
     *
     * @param id
     * @return
     * @see #converterFactory(KeycloakSession, String)
     */
    public ConverterFactory converterFactory(String id) {
        return converterFactory(session, id);
    }

    /**
     * Validates the {@link ConverterConfig} of {@link Converter} referenced by the given provider {@code id}.
     *
     * @param id
     * @param config
     * @return
     * @see #validateConfig(KeycloakSession, String, ConverterConfig)
     */
    public ValidationResult validateConfig(String id, ConverterConfig config) {
        return validateConfig(session, id, config);
    }

    /**
     * Look-up up for a built-in or registered {@link Converter} with the given converterId.
     *
     * @param session the {@link KeycloakSession}
     * @param id      the id of the converter
     * @return the {@link Converter} or {@literal null}
     */
    public static Converter converter(KeycloakSession session, String id) {
        if (session == null) {
            throw new IllegalArgumentException("KeycloakSession must be not null");
        }

        // Lookup converter in registry
        return session.getProvider(Converter.class, id);
    }

    /**
     * Look-up for a built-in or registered {@link ConverterFactory} with the given converterId.
     * <p>
     * This is intended for users who want to dynamically create new {@link Converter} instances, validate
     * {@link ConverterConfig} configurations or create default configurations for a {@link Converter}.
     *
     * @param session the {@link KeycloakSession}
     * @param id      the id of the converter
     * @return the {@link Converter} or {@literal null}
     */
    public static ConverterFactory converterFactory(KeycloakSession session, String id) {
        if (session == null) {
            throw new IllegalArgumentException("KeycloakSession must be not null");
        }

        // Lookup factory in registry
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        return (ConverterFactory) sessionFactory.getProviderFactory(Converter.class, id);
    }

    /**
     * Validates the {@link ConverterConfig} of {@link Converter} referenced by the given provider {@code id}.
     *
     * @param session
     * @param id of the converter
     * @param config to be validated
     * @return
     */
    public static ValidationResult validateConfig(KeycloakSession session, String id, ConverterConfig config) {

        ConverterFactory converterFactory = converterFactory(session, id);
        if (converterFactory != null) {
            return converterFactory.validateConfig(session, config);
        }

        // We could not find a ConverterFactory to validate that config, so we assume the config is valid.
        return ValidationResult.OK;
    }
}
