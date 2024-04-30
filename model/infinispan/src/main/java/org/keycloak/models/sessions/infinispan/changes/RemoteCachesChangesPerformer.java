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

package org.keycloak.models.sessions.infinispan.changes;

import org.infinispan.Cache;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheInvoker;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class RemoteCachesChangesPerformer<K, V extends SessionEntity> implements SessionChangesPerformer<K, V> {

    private final KeycloakSession session;
    private final Cache<K, SessionEntityWrapper<V>> cache;
    private final RemoteCacheInvoker remoteCacheInvoker;
    private final List<Runnable> changes = new LinkedList<>();


    public RemoteCachesChangesPerformer(KeycloakSession session, Cache<K, SessionEntityWrapper<V>> cache, RemoteCacheInvoker remoteCacheInvoker) {
        this.session = session;
        this.cache = cache;
        this.remoteCacheInvoker = remoteCacheInvoker;
    }

    @Override
    public void registerChange(Map.Entry<K, SessionUpdatesList<V>> entry, MergedUpdate<V> merged) {
        SessionUpdatesList<V> updates = entry.getValue();
        changes.add(() -> remoteCacheInvoker.runTask(session, updates.getRealm(), cache.getName(), entry.getKey(), merged, updates.getEntityWrapper()));
    }

    @Override
    public void applyChanges() {
        changes.forEach(Runnable::run);
    }
}
