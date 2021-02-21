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
package org.keycloak.component;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.InvalidationHandler;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import java.util.function.Function;
import org.keycloak.component.ComponentModel;

/**
 *
 * @author hmlnarik
 */
public interface ComponentFactoryProviderFactory extends ProviderFactory<ComponentFactoryProvider>, InvalidationHandler {

    <T extends Provider> ProviderFactory<T> getProviderFactory(Class<T> clazz, String realmId, String componentId, Function<KeycloakSessionFactory, ComponentModel> model);

    @Override
    default ComponentFactoryProvider create(KeycloakSession session) {
        throw new UnsupportedOperationException("ComponentFactoryProvider is session-independent, hence not instantiable per session.");
    }

}
