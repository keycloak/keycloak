/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.cluster;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;

/**
 * A Service Provider Interface (SPI) that allows to plug-in an embedded or remote cache manager instance.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface ManagedCacheManagerProvider {

    <C> C getEmbeddedCacheManager(KeycloakSession keycloakSession, Config.Scope config);

    /**
     * @return A RemoteCacheManager if the features {@link org.keycloak.common.Profile.Feature#CLUSTERLESS} or {@link org.keycloak.common.Profile.Feature#MULTI_SITE}  is enabled, {@code null} otherwise.
     * @deprecated The RemoteCacheManager is created and managed by keycloak. Use InfinispanConnectionProvider to retrieve it and implement CacheRemoteConfigProvider to overwrite the configuration.
     */
    @Deprecated(since = "26.3", forRemoval = true)
    default <C> C getRemoteCacheManager(Config.Scope config) {
        return null;
    }
}
