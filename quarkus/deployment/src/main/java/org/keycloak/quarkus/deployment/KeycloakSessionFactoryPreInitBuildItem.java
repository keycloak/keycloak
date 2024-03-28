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

package org.keycloak.quarkus.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.theme.ClasspathThemeProviderFactory;

import java.util.List;
import java.util.Map;

/**
 * A symbolic build item that can be consumed by other build steps when the {@link org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory}
 * is pre-initialized.
 */
public final class KeycloakSessionFactoryPreInitBuildItem extends SimpleBuildItem {
    private Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories;
    private Map<Class<? extends Provider>, String> defaultProviders;
    private Map<String, ProviderFactory> preConfiguredProviders;
    private List<ClasspathThemeProviderFactory.ThemesRepresentation> themes;

    public KeycloakSessionFactoryPreInitBuildItem(Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories, Map<Class<? extends Provider>, String> defaultProviders, Map<String, ProviderFactory> preConfiguredProviders, List<ClasspathThemeProviderFactory.ThemesRepresentation> themes) {
        this.factories = factories;
        this.defaultProviders = defaultProviders;
        this.preConfiguredProviders = preConfiguredProviders;
        this.themes = themes;
    }

    public Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> getFactories() {
        return factories;
    }

    public Map<Class<? extends Provider>, String> getDefaultProviders() {
        return defaultProviders;
    }

    public Map<String, ProviderFactory> getPreConfiguredProviders() {
        return preConfiguredProviders;
    }

    public List<ClasspathThemeProviderFactory.ThemesRepresentation> getThemes() {
        return themes;
    }
}
