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

package org.keycloak.provider;

import java.util.Set;

/**
 * Holds the {@link ProviderFactory} classes discovered at build time via the
 * {@link KeycloakProvider} annotation scan in the Quarkus deployment processor.
 *
 * The registry is populated once via {@link #install(Set)} during Quarkus augmentation,
 * before {@link DefaultProviderLoader} is consulted. Outside the Quarkus build path
 * (embedded usage, tests that don't go through augmentation) the registry stays empty
 * and discovery degrades to {@link java.util.ServiceLoader} alone.
 */
public final class GeneratedProviderRegistry {

    private static volatile Set<Class<? extends ProviderFactory>> factoryClasses = Set.of();

    private GeneratedProviderRegistry() {
    }

    /**
     * Returns the {@link ProviderFactory} classes discovered at build time, or an empty set
     * if the scan has not been installed (non-Quarkus contexts).
     */
    public static Set<Class<? extends ProviderFactory>> getProviderFactoryClasses() {
        return factoryClasses;
    }

    /**
     * Install the result of the build-time scan. Called exactly once by the Quarkus
     * deployment processor before any {@link DefaultProviderLoader#load(Spi)} call.
     */
    public static void install(Set<Class<? extends ProviderFactory>> classes) {
        factoryClasses = Set.copyOf(classes);
    }
}
