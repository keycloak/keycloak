/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.provider.ProviderFactory;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Carries the {@link ProviderFactory} classes discovered by the build-time
 * {@link org.keycloak.provider.KeycloakProvider} annotation scan, keyed by the provider factory
 * interface (the SPI) they were annotated for. Consumed by
 * {@link KeycloakProcessor#configureKeycloakSessionFactory} so that {@code loadFactories()}
 * registers them with the {@link org.keycloak.provider.KeycloakDeploymentInfo} used for
 * provider discovery.
 */
public final class ProviderRegistryBuildItem extends SimpleBuildItem {

    private final Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> providerFactoryClasses;

    public ProviderRegistryBuildItem(Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> providerFactoryClasses) {
        // preserve the deterministic order established by the scan
        Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> copy = new LinkedHashMap<>();
        providerFactoryClasses.forEach((factoryInterface, classes) ->
                copy.put(factoryInterface, Collections.unmodifiableSet(new LinkedHashSet<>(classes))));
        this.providerFactoryClasses = Collections.unmodifiableMap(copy);
    }

    /**
     * @return discovered factory classes keyed by the provider factory interface they are registered for
     */
    public Map<Class<? extends ProviderFactory>, Set<Class<? extends ProviderFactory>>> getProviderFactoryClasses() {
        return providerFactoryClasses;
    }
}
