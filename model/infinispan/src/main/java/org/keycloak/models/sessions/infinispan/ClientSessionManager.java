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

package org.keycloak.models.sessions.infinispan;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.sessions.infinispan.changes.PersistentSessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;

/**
 * Manages transactional context for {@link AuthenticatedClientSessionModel} changes.
 * <p>
 * It collects all modifications (the changelog) within the current transaction and applies them to the database only
 * upon a successful commit.
 */
public interface ClientSessionManager {

    /**
     * Adds a update task to the changelog for a specific client session.
     * <p>
     * When the transaction commits, this task will apply its changes to the persisted
     * {@link AuthenticatedClientSessionEntity}, effectively updating the corresponding
     * {@link AuthenticatedClientSessionModel}. Multiple {@code addChange} calls for the same session are accumulated
     * (merged).
     *
     * @param key  The identifier for the target client session.
     * @param task The operation containing the changes to apply to the persisted entity.
     * @throws NullPointerException if {@code key} or {@code task} is {@code null}.
     */
    void addChange(EmbeddedClientSessionKey key, PersistentSessionUpdateTask<AuthenticatedClientSessionEntity> task);

    /**
     * Resets and replaces the state of the persisted {@link AuthenticatedClientSessionEntity} for the given session.
     * <p>
     * All previously added changes via {@code addChange} for this session are discarded, and the provided task is
     * executed to set the new state of the client session entity.
     *
     * @param key  The identifier for the target client session.
     * @param task The operation that must set the complete new state of the persisted entity.
     * @throws NullPointerException if {@code key} or {@code task} is {@code null}.
     */
    void restartEntity(EmbeddedClientSessionKey key, PersistentSessionUpdateTask<AuthenticatedClientSessionEntity> task);

}
