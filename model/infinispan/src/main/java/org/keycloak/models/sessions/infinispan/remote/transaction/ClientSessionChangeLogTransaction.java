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

import java.util.UUID;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.sessions.infinispan.changes.remote.remover.iteration.ByRealmIdConditionalRemover;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.UpdaterFactory;
import org.keycloak.models.sessions.infinispan.changes.remote.updater.client.AuthenticatedClientSessionUpdater;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;

/**
 * Syntactic sugar for
 * {@code RemoteChangeLogTransaction<SessionKey, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater,
 * UserAndClientSessionConditionalRemover<AuthenticatedClientSessionEntity>>}
 */
public class ClientSessionChangeLogTransaction extends RemoteChangeLogTransaction<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater, ByRealmIdConditionalRemover<UUID, AuthenticatedClientSessionEntity>> {

    public ClientSessionChangeLogTransaction(UpdaterFactory<UUID, AuthenticatedClientSessionEntity, AuthenticatedClientSessionUpdater> factory, RemoteCache<UUID, AuthenticatedClientSessionEntity> cache) {
        super(factory, cache, new ByRealmIdConditionalRemover<>());
    }

}
