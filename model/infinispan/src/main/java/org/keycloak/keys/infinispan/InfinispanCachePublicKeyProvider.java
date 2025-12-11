/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keys.infinispan;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.cache.CachePublicKeyProvider;
import org.keycloak.models.cache.infinispan.ClearCacheEvent;

import org.infinispan.Cache;

public class InfinispanCachePublicKeyProvider implements CachePublicKeyProvider {

    private final KeycloakSession session;

    private final Cache<String, PublicKeysEntry> keys;

    public InfinispanCachePublicKeyProvider(KeycloakSession session, Cache<String, PublicKeysEntry> keys) {
        this.session = session;
        this.keys = keys;
    }

    @Override
    public void clearCache() {
        keys.clear();
        ClusterProvider cluster = session.getProvider(ClusterProvider.class);
        cluster.notify(InfinispanCachePublicKeyProviderFactory.KEYS_CLEAR_CACHE_EVENTS, ClearCacheEvent.getInstance(), true);
    }

    @Override
    public void close() {

    }
}
