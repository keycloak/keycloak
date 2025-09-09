/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oauth2.resourceindicators;

import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;

import java.util.Set;

/**
 * Provider to customize OAuth2 Resource Indicators resolving.
 *
 * @link <a href="https://www.rfc-editor.org/rfc/rfc8707">RFC 8707 OAuth2 Resource Indicators</a>
 */
public interface OAuth2ResourceIndicatorResolver extends Provider {

    /**
     * Returns a client associated with the given {@code resourceIndicator} or {@literal null} if no client could be found.
     * @param resourceIndicator
     * @return client model
     */
    ClientModel findClientByResourceIndicator(String resourceIndicator);

    /**
     * Filters the given resource indicators to the supported resource indicators by the given {@link ClientModel client}.
     *
     * @param client client to find the resource indicators
     * @param resourceIndicatorCandidates resource indicators to check
     *
     * @return Set of the resource indicators narrowed down to resource indicators that are supported by the given client.
     */
    Set<String> narrowResourceIndicators(ClientModel client, Set<String> resourceIndicatorCandidates);

    @Override
    default void close() {
    }
}
