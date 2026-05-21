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
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds the fully-qualified class names of {@link org.keycloak.provider.ProviderFactory}
 * implementations discovered at build time via the {@link org.keycloak.provider.KeycloakProvider}
 * annotation.
 */
public final class ProviderRegistryBuildItem extends SimpleBuildItem {

    private final Set<String> providerFactoryClassNames;

    public ProviderRegistryBuildItem(Set<String> providerFactoryClassNames) {
        this.providerFactoryClassNames = Collections.unmodifiableSet(new TreeSet<>(providerFactoryClassNames));
    }

    public Set<String> getProviderFactoryClassNames() {
        return providerFactoryClassNames;
    }
}
