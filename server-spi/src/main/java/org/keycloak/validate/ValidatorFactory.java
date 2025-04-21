/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validate;

import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

/**
 * A factory for custom {@link Validator} implementations plugged-in through this SPI.
 */
public interface ValidatorFactory extends ProviderFactory<Validator> {

    /**
     * Validates the given validation config.
     * <p>
     * Implementations can use the {@link KeycloakSession} to validate the given {@link ValidatorConfig}.
     *
     * @param session the {@link KeycloakSession}
     * @param config  the config to be validated
     * @return the validation result
     */
    default ValidationResult validateConfig(KeycloakSession session, ValidatorConfig config) {
        return ValidationResult.OK;
    }

    /**
     * This is called when the server shuts down.
     */
    @Override
    default void close() {
        // NOOP
    }
}
