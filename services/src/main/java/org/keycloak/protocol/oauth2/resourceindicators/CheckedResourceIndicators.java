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

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents the result of narrowing a set of requested resource indicators to supported resource indicators.
 * 
 * See {@link ResourceIndicatorsUtil#narrowResourceIndicators(KeycloakSession, ClientModel, AuthenticatedClientSessionModel, Set)}
 */
public class CheckedResourceIndicators {

    /**
     * Represents an empty result.
     */
    public static final CheckedResourceIndicators EMPTY = new CheckedResourceIndicators(Collections.emptySet(), Collections.emptySet());

    private final Set<String> supportedResources;

    private final Set<String> unsupportedResources;

    private final Set<String> requestedResources;

    /**
     * Creates a new {@link CheckedResourceIndicators}.
     * @param allowedResources the set of allowed resource indicators
     * @param requestedResources the set of requested resource indicators.
     */
    public CheckedResourceIndicators(Set<String> allowedResources, Set<String> requestedResources) {
        var supported = new HashSet<>(allowedResources);
        supported.retainAll(requestedResources);
        this.supportedResources = supported;
        var unsupported = new HashSet<>(requestedResources);
        unsupported.removeAll(allowedResources);
        this.unsupportedResources = unsupported;
        this.requestedResources = requestedResources;
    }

    public Set<String> getRequestedResources() {
        return requestedResources;
    }

    public Set<String> getSupportedResources() {
        return supportedResources;
    }

    public Set<String> getUnsupportedResources() {
        return unsupportedResources;
    }

    public boolean hasSupportedResources() {
        return supportedResources != null && !supportedResources.isEmpty();
    }

    public boolean hasUnsupportedResources() {
        return unsupportedResources != null && !unsupportedResources.isEmpty();
    }
}
