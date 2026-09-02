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

package org.keycloak.spi.infinispan;

import java.util.Optional;

import org.keycloak.provider.Provider;

import org.infinispan.client.hotrod.configuration.Configuration;

/**
 * A provider to create a configuration to the Hot Rod client.
 */
public interface CacheRemoteConfigProvider extends Provider {

    /**
     * Creates the {@link Configuration} for the Hot Rod client.
     * <p>
     * The optional signal if a Hot Rod client should be instantiated and started. If present, it assumes an external
     * Infinispan cluster is ready and online, otherwise Keycloak fails to start.
     *
     * @return The {@link Configuration} for the Hot Rod client.
     */
    Optional<Configuration> configuration();

    @Override
    default void close() {
        //no-op
    }
}
