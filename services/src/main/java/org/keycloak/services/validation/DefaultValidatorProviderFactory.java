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
package org.keycloak.services.validation;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.validation.ValidationProvider;
import org.keycloak.validation.ValidationRegistry;
import org.keycloak.validation.ValidatorProvider;
import org.keycloak.validation.ValidatorProviderFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DefaultValidatorProviderFactory implements ValidatorProviderFactory {

    private ValidationRegistry validatorRegistry;

    @Override
    public ValidatorProvider create(KeycloakSession session) {
        return new DefaultValidatorProvider(session, validatorRegistry);
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // TODO discuss lazily constructing the ValidatorRegistry in #create(KeycloakSession) method instead of here
        this.validatorRegistry = createValidatorRegistry(keycloakSessionFactory);
    }

    @SuppressWarnings("rawtypes")
    protected ValidationRegistry createValidatorRegistry(KeycloakSessionFactory keycloakSessionFactory) {

        DefaultValidationRegistry validatorRegistry = new DefaultValidationRegistry();

        KeycloakSession keycloakSession = keycloakSessionFactory.create();
        List<ProviderFactory> providerFactories = keycloakSessionFactory.getProviderFactories(ValidationProvider.class);

        providerFactories.sort(Comparator.comparing(ProviderFactory::order));

        // Loop over all ValidationProvider registries to register all visible Validation implementations
        for (ProviderFactory providerFactory : providerFactories) {
            providerFactory.postInit(keycloakSessionFactory);
            ValidationProvider validatorProvider = (ValidationProvider) providerFactory.create(keycloakSession);
            validatorProvider.register(validatorRegistry);
        }

        return validatorRegistry;
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return "default";
    }
}
