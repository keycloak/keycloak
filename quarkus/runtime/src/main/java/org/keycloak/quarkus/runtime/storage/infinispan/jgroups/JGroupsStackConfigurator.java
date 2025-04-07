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

package org.keycloak.quarkus.runtime.storage.infinispan.jgroups;

import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.keycloak.models.KeycloakSession;

/**
 * Interface to configure a JGroups Stack before Keycloak starts the embedded Infinispan.
 */
public interface JGroupsStackConfigurator {

    /**
     * @return {@code true} if this configuration requires the sessions, for example, to access a database.
     */
    boolean requiresKeycloakSession();

    /**
     * Configures the stack in {@code holder}.
     * <p>
     * The {@code session} is not {@code null} when {@link #requiresKeycloakSession()} returns {@code true}.
     *
     * @param holder  The Infinispan {@link ConfigurationBuilderHolder}.
     * @param session The current {@link KeycloakSession}. It may be {@code null};
     */
    void configure(ConfigurationBuilderHolder holder, KeycloakSession session);

}
