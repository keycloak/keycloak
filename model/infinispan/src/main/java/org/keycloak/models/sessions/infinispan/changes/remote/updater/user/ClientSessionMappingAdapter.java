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

package org.keycloak.models.sessions.infinispan.changes.remote.updater.user;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * This class adapts and converts the {@link UserSessionEntity#getAuthenticatedClientSessions()} into
 * {@link UserSessionModel#getAuthenticatedClientSessions()}.
 * <p>
 * Its implementation optimizes methods {@link #clear()}, {@link #put(String, AuthenticatedClientSessionModel)},
 * {@link #get(Object)} and {@link #remove(Object)} by avoiding download all client sessions from the
 * {@link RemoteCache}.
 * <p>
 * The remaining methods are more expensive and require downloading all client sessions. The requests are done in
 * concurrently to reduce the overall response time.
 * <p>
 * This class keeps track of any modification required in {@link UserSessionEntity#getAuthenticatedClientSessions()} and
 * those modification can be replayed.
 */
public class ClientSessionMappingAdapter extends AbstractMap<String, AuthenticatedClientSessionModel> {

    private static final Consumer<AuthenticatedClientSessionStore> CLEAR = AuthenticatedClientSessionStore::clear;

    private final AuthenticatedClientSessionStore mappings;
    private final ClientSessionProvider clientSessionProvider;
    private final List<Consumer<AuthenticatedClientSessionStore>> changes;

    public ClientSessionMappingAdapter(AuthenticatedClientSessionStore mappings, ClientSessionProvider clientSessionProvider) {
        this.mappings = Objects.requireNonNull(mappings);
        this.clientSessionProvider = Objects.requireNonNull(clientSessionProvider);
        changes = new CopyOnWriteArrayList<>();
    }

    @Override
    public void clear() {
        mappings.forEach((id, uuid) -> clientSessionProvider.removeClientSession(uuid));
        changes.clear();
        addChangeAndApply(CLEAR);
    }

    @Override
    public AuthenticatedClientSessionModel put(String key, AuthenticatedClientSessionModel value) {
        addChangeAndApply(store -> store.put(key, UUID.fromString(value.getId())));
        return clientSessionProvider.getClientSession(key, mappings.get(key));
    }

    @Override
    public AuthenticatedClientSessionModel remove(Object key) {
        var clientId = String.valueOf(key);
        var uuid = mappings.get(clientId);
        var existing = clientSessionProvider.getClientSession(clientId, uuid);
        onClientRemoved(clientId, uuid);
        return existing;
    }

    @Override
    public AuthenticatedClientSessionModel get(Object key) {
        var clientId = String.valueOf(key);
        return clientSessionProvider.getClientSession(clientId, mappings.get(clientId));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Set<Entry<String, AuthenticatedClientSessionModel>> entrySet() {
        Map<String, AuthenticatedClientSessionModel> results = new ConcurrentHashMap<>(mappings.size());
        var stage = CompletionStages.aggregateCompletionStage();
        mappings.forEach((clientId, uuid) -> stage.dependsOn(clientSessionProvider.getClientSessionAsync(clientId, uuid)
                .thenAccept(updater -> {
                    if (updater == null) {
                        onClientRemoved(clientId, uuid);
                        return;
                    }
                    results.put(clientId, updater);
                })));
        CompletionStages.join(stage.freeze());
        return results.entrySet();
    }

    boolean isUnchanged() {
        return changes.isEmpty();
    }

    void removeAll(Collection<String> removedClientUUIDS) {
        if (removedClientUUIDS == null || removedClientUUIDS.isEmpty()) {
            return;
        }
        removedClientUUIDS.forEach(this::onClientRemoved);
    }

    /**
     * Applies the modifications recorded by this class into a different {@link AuthenticatedClientSessionStore}.
     *
     * @param store The {@link AuthenticatedClientSessionStore} to update.
     */
    void applyChanges(AuthenticatedClientSessionStore store) {
        changes.forEach(change -> change.accept(store));
    }

    private void addChangeAndApply(Consumer<AuthenticatedClientSessionStore> change) {
        change.accept(mappings);
        changes.add(change);
    }

    private void onClientRemoved(String clientId) {
        onClientRemoved(clientId, mappings.get(clientId));
    }

    private void onClientRemoved(String clientId, UUID key) {
        addChangeAndApply(store -> store.remove(clientId));
        clientSessionProvider.removeClientSession(key);
    }
}
