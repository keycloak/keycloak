/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.storage.attributes;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

import java.util.Collections;
import java.util.List;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Service provider interface fetching attributes from external data stores. This allows keycloak to enrich both user attributes
 * and keycloak generated tokens with attributes from external locations.
 */
public class AttributeStoreProviderSpi implements Spi {

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return "attribute-storage";
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AttributeStoreProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AttributeStoreProviderFactory.class;
    }

    public static List<ProviderConfigProperty> commonConfig() {
        return Collections.emptyList();
    }

}
