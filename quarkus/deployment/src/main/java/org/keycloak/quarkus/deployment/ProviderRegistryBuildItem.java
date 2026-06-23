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

import io.quarkus.builder.item.EmptyBuildItem;

/**
 * Ordering marker produced once the {@link org.keycloak.provider.KeycloakProvider}
 * annotation scan has run and the build-time
 * {@link org.keycloak.provider.GeneratedProviderRegistry} install has happened.
 * Consumed by {@link KeycloakProcessor#configureKeycloakSessionFactory(...)} so that
 * {@code loadFactories()} sees the discovered factories.
 */
public final class ProviderRegistryBuildItem extends EmptyBuildItem {
}
