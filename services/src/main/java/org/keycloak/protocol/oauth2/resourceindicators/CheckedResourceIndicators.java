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

    private final Set<String> supported;

    private final Set<String> requested;

    private final Set<String> unsupported;

    /**
     * Creates a new {@link CheckedResourceIndicators}.
     * @param supported the set of supported resource indicators
     * @param requested the set of requested resource indicators.
     */
    public CheckedResourceIndicators(Set<String> supported, Set<String> requested) {
        this.supported = supported;
        this.requested = requested;
        Set<String> unsupported = new HashSet<>(requested);
        unsupported.removeAll(supported);
        this.unsupported = unsupported;
    }

    public Set<String> getRequested() {
        return requested;
    }

    public Set<String> getSupported() {
        return supported;
    }

    public Set<String> getUnsupported() {
        return unsupported;
    }

    public boolean hasSupported() {
        return supported != null && !supported.isEmpty();
    }

    public boolean hasUnsupported() {
        return unsupported != null && !unsupported.isEmpty();
    }
}
