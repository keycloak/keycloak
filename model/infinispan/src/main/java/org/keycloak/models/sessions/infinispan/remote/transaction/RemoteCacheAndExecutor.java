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

package org.keycloak.models.sessions.infinispan.remote.transaction;

import java.util.concurrent.Executor;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public record RemoteCacheAndExecutor<K, V>(RemoteCache<K, V> cache, Executor executor) {

    public static <K1, V1> RemoteCacheAndExecutor<K1, V1> create(KeycloakSession session, String cacheName) {
        var connection = session.getProvider(InfinispanConnectionProvider.class);
        return new RemoteCacheAndExecutor<>(connection.getRemoteCache(cacheName), connection.getExecutor(cacheName + "-query-delete"));
    }

    public static <K1, V1> RemoteCacheAndExecutor<K1, V1> create(KeycloakSessionFactory factory, String cacheName) {
        try (var session = factory.create()) {
            return create(session, cacheName);
        }
    }
}
