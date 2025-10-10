/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication;

import org.keycloak.events.EventBuilder;
import org.keycloak.provider.Provider;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Optional;

/**
 * Determines the requested level of authentication for the current request, for example using acr_values.
 */
public interface RequestedLevelOfAuthenticationProvider extends Provider {
    /**
     * Determines the current requested level of authentication.
     *
     * @param acrValue provided acr-value
     * @return Numerical level of authentication or Optional.empty, if no mapping is possible
     */
    Optional<Integer> getRequestedLoa(String acrValue);
}
