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

import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.infinispan.client.hotrod.RemoteCache;
import org.keycloak.models.AuthenticatedClientSessionModel;

/**
 * An SPI for {@link ClientSessionMappingAdapter} to interact with the {@link RemoteCache}.
 */
public interface ClientSessionProvider {

    /**
     * Synchronously fetch an {@link AuthenticatedClientSessionModel} from the {@link RemoteCache}.
     *
     * @param clientId        The client's ID.
     * @param clientSessionId The {@link RemoteCache} key.
     * @return The {@link AuthenticatedClientSessionModel} instance or {@code null} if the client session does not exist
     * or was removed.
     */
    AuthenticatedClientSessionModel getClientSession(String clientId, UUID clientSessionId);

    /**
     * A non-blocking alternative to {@link #getClientSession(String, UUID)}.
     *
     * @see #getClientSession(String, UUID)
     */
    CompletionStage<AuthenticatedClientSessionModel> getClientSessionAsync(String clientId, UUID clientSessionId);

    /**
     * Removes the client session associated with the {@link RemoteCache} key.
     * <p>
     * If {@code clientSessionId} is {@code null}, nothing is removed. The methods
     * {@link #getClientSession(String, UUID)} and {@link #getClientSessionAsync(String, UUID)} will return {@code null}
     * for the session after this method is completed.
     *
     * @param clientSessionId The {@link RemoteCache} key to remove.
     */
    void removeClientSession(UUID clientSessionId);

}
